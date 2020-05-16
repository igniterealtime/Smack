/**
 *
 * Copyright 2018-2020 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SmackReactor for non-blocking I/O.
 * <p>
 * Highlights include:
 * <ul>
 * <li>Multiple reactor threads</li>
 * <li>Scheduled actions</li>
 * </ul>
 *
 * <pre>
 *
 *           ) ) )
 *        ( ( (
 *      ) ) )
 *   (~~~~~~~~~)
 *    | Smack |
 *    |Reactor|
 *    I      _._
 *    I    /'   `\
 *    I   |       |
 *    f   |   |~~~~~~~~~~~~~~|
 *  .'    |   | #   #   #  # |
 * '______|___|___________###|
 * </pre>
 */
public class SmackReactor {

    private static final Logger LOGGER = Logger.getLogger(SmackReactor.class.getName());

    private static final int DEFAULT_REACTOR_THREAD_COUNT = 2;

    private static final int PENDING_SET_INTEREST_OPS_MAX_BATCH_SIZE = 1024;

    private static SmackReactor INSTANCE;

    static synchronized SmackReactor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SmackReactor("DefaultReactor");
        }
        return INSTANCE;
    }

    private final Selector selector;
    private final String reactorName;

    private final List<Reactor> reactorThreads = Collections.synchronizedList(new ArrayList<>());

    private final DelayQueue<ScheduledAction> scheduledActions = new DelayQueue<>();

    private final Lock registrationLock = new ReentrantLock();

    /**
     * The semaphore protecting the handling of the actions. Note that it is
     * initialized with -1, which basically means that one thread will always do I/O using
     * select().
     */
    private final Semaphore actionsSemaphore = new Semaphore(-1, false);

    private final Queue<SelectionKey> pendingSelectionKeys = new ConcurrentLinkedQueue<>();

    private final Queue<SetInterestOps> pendingSetInterestOps = new ConcurrentLinkedQueue<>();

    SmackReactor(String reactorName) {
        this.reactorName = reactorName;

        try {
            selector = Selector.open();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        setReactorThreadCount(DEFAULT_REACTOR_THREAD_COUNT);
    }

    public SelectionKey registerWithSelector(SelectableChannel channel, int ops, ChannelSelectedCallback callback)
            throws ClosedChannelException {
        SelectionKeyAttachment selectionKeyAttachment = new SelectionKeyAttachment(callback);

        registrationLock.lock();
        try {
            selector.wakeup();
            return channel.register(selector, ops, selectionKeyAttachment);
        } finally {
            registrationLock.unlock();
        }
    }

    public void setInterestOps(SelectionKey selectionKey, int interestOps) {
        SetInterestOps setInterestOps = new SetInterestOps(selectionKey, interestOps);
        pendingSetInterestOps.add(setInterestOps);
        selector.wakeup();
    }

    private static final class SetInterestOps {
        private final SelectionKey selectionKey;
        private final int interestOps;

        private SetInterestOps(SelectionKey selectionKey, int interestOps) {
            this.selectionKey = selectionKey;
            this.interestOps = interestOps;
        }
    }

    ScheduledAction schedule(Runnable runnable, long delay, TimeUnit unit) {
        long releaseTimeEpoch = System.currentTimeMillis() + unit.toMillis(delay);
        Date releaseTimeDate = new Date(releaseTimeEpoch);
        ScheduledAction scheduledAction = new ScheduledAction(runnable, releaseTimeDate, this);
        scheduledActions.add(scheduledAction);
        selector.wakeup();
        return scheduledAction;
    }

    /**
     * Cancels the scheduled action.
     *
     * @param scheduledAction the scheduled action to cancel.
     * @return <code>true</code> if the scheduled action was still pending and got removed, <code>false</code> otherwise.
     */
    boolean cancel(ScheduledAction scheduledAction) {
        return scheduledActions.remove(scheduledAction);
    }

    private class Reactor extends Thread {

        private volatile long shutdownRequestTimestamp = -1;

        @Override
        public void run() {
            try {
                reactorLoop();
            } finally {
                if (shutdownRequestTimestamp > 0) {
                    long shutDownDelay = System.currentTimeMillis() - shutdownRequestTimestamp;
                    LOGGER.info(this + " shut down after " + shutDownDelay + "ms");
                } else {
                    boolean contained = reactorThreads.remove(this);
                    assert contained;
                }
            }
        }

        private void reactorLoop() {
            // Loop until reactor shutdown was requested.
            while (shutdownRequestTimestamp < 0) {
                handleScheduledActionsOrPerformSelect();

                handlePendingSelectionKeys();
            }
        }

        @SuppressWarnings("LockNotBeforeTry")
        private void handleScheduledActionsOrPerformSelect() {
            ScheduledAction dueScheduledAction = null;

            boolean permitToHandleScheduledActions = actionsSemaphore.tryAcquire();
            if (permitToHandleScheduledActions) {
                try {
                    dueScheduledAction = scheduledActions.poll();
                } finally {
                    actionsSemaphore.release();
                }
            }

            if (dueScheduledAction != null) {
                dueScheduledAction.action.run();
                return;
            }

            int newSelectedKeysCount = 0;
            List<SelectionKey> selectedKeys;
            synchronized (selector) {
                ScheduledAction nextScheduledAction = scheduledActions.peek();

                long selectWait;
                if (nextScheduledAction == null) {
                    // There is no next scheduled action, wait indefinitely in select() or until another thread invokes
                    // selector.wakeup().
                    selectWait = 0;
                } else {
                    selectWait = nextScheduledAction.getTimeToDueMillis();
                }

                if (selectWait < 0) {
                    // A scheduled action was just released and became ready to execute.
                    return;
                }

                // Before we call select, we handle the pending the interest Ops. This will not block since no other
                // thread is currently in select() at this time.
                // Note: This was put deliberately before the registration lock. It may cause more synchronization but
                // allows for more parallelism.
                // Hopefully that assumption is right.
                int myHandledPendingSetInterestOps = 0;
                for (SetInterestOps setInterestOps; (setInterestOps = pendingSetInterestOps.poll()) != null;) {
                    setInterestOpsCancelledKeySafe(setInterestOps.selectionKey, setInterestOps.interestOps);

                    if (myHandledPendingSetInterestOps++ >= PENDING_SET_INTEREST_OPS_MAX_BATCH_SIZE) {
                        // This thread has handled enough "set pending interest ops" requests. Wakeup another one to
                        // handle the remaining (if any).
                        selector.wakeup();
                        break;
                    }
                }

                // Ensure that a wakeup() in registerWithSelector() gives the corresponding
                // register() in the same method the chance to actually register the channel. In
                // other words: This construct ensures that there is never another select()
                // between a corresponding wakeup() and register() calls.
                // See also https://stackoverflow.com/a/1112809/194894
                registrationLock.lock();
                registrationLock.unlock();

                try {
                    newSelectedKeysCount = selector.select(selectWait);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "IOException while using select()", e);
                    return;
                }

                if (newSelectedKeysCount == 0) {
                    return;
                }

                // Copy the selected-key set over to selectedKeys, remove the keys from the
                // selected key set and loose interest of the key OPs for the time being.
                // Note that we perform this operation in two steps in order to maximize the
                // timespan setRacing() is set.
                Set<SelectionKey> selectedKeySet = selector.selectedKeys();
                for (SelectionKey selectionKey : selectedKeySet) {
                    SelectionKeyAttachment selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();
                    selectionKeyAttachment.setRacing();
                }
                for (SelectionKey selectionKey : selectedKeySet) {
                    setInterestOpsCancelledKeySafe(selectionKey, 0);
                }

                selectedKeys = new ArrayList<>(selectedKeySet.size());
                selectedKeys.addAll(selectedKeySet);
                selectedKeySet.clear();
            }

            int selectedKeysCount = selectedKeys.size();
            int currentReactorThreadCount = reactorThreads.size();
            int myKeyCount;
            if (selectedKeysCount > currentReactorThreadCount) {
                myKeyCount = selectedKeysCount / currentReactorThreadCount;
            } else {
                myKeyCount = selectedKeysCount;
            }

            final Level reactorSelectStatsLogLevel = Level.FINE;
            if (LOGGER.isLoggable(reactorSelectStatsLogLevel)) {
                LOGGER.log(reactorSelectStatsLogLevel,
                                "New selected key count: " + newSelectedKeysCount
                                + ". Total selected key count " + selectedKeysCount
                                + ". My key count: " + myKeyCount
                                + ". Current reactor thread count: " + currentReactorThreadCount);
            }

            Collection<SelectionKey> mySelectedKeys = new ArrayList<>(myKeyCount);
            Iterator<SelectionKey> it = selectedKeys.iterator();
            for (int i = 0; i < myKeyCount; i++) {
                SelectionKey selectionKey = it.next();
                mySelectedKeys.add(selectionKey);
            }
            while (it.hasNext()) {
                // Drain to pendingSelectionKeys.
                SelectionKey selectionKey = it.next();
                pendingSelectionKeys.add(selectionKey);
            }

            if (selectedKeysCount - myKeyCount > 0) {
                // There where pending selection keys: Wakeup another reactor thread to handle them.
                selector.wakeup();
            }

            handleSelectedKeys(mySelectedKeys);
        }

        private void handlePendingSelectionKeys() {
            final int pendingSelectionKeysSize = pendingSelectionKeys.size();
            if (pendingSelectionKeysSize == 0) {
                return;
            }

            int currentReactorThreadCount = reactorThreads.size();
            int myKeyCount = pendingSelectionKeysSize / currentReactorThreadCount;
            Collection<SelectionKey> selectedKeys = new ArrayList<>(myKeyCount);
            for (int i = 0; i < myKeyCount; i++) {
                SelectionKey selectionKey = pendingSelectionKeys.poll();
                if (selectionKey == null) {
                    // We lost a race and can abort here since the pendingSelectionKeys queue is empty.
                    break;
                }
                selectedKeys.add(selectionKey);
            }

            if (!pendingSelectionKeys.isEmpty()) {
                // There are more pending selection keys, wakeup a thread blocked in select() to handle them.
                selector.wakeup();
            }

            handleSelectedKeys(selectedKeys);
        }

        private void setInterestOpsCancelledKeySafe(SelectionKey selectionKey, int interestOps) {
            try {
                selectionKey.interestOps(interestOps);
            }
            catch (CancelledKeyException e) {
                final Level keyCancelledLogLevel = Level.FINER;
                if (LOGGER.isLoggable(keyCancelledLogLevel)) {
                    LOGGER.log(keyCancelledLogLevel, "Key '" + selectionKey + "' has been cancelled", e);
                }
            }
        }

        void requestShutdown() {
            shutdownRequestTimestamp = System.currentTimeMillis();
        }
    }

    private static void handleSelectedKeys(Collection<SelectionKey> selectedKeys) {
        for (SelectionKey selectionKey : selectedKeys) {
            SelectableChannel channel = selectionKey.channel();
            SelectionKeyAttachment selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();
            ChannelSelectedCallback channelSelectedCallback = selectionKeyAttachment.weaeklyReferencedChannelSelectedCallback.get();
            if (channelSelectedCallback != null) {
                channelSelectedCallback.onChannelSelected(channel, selectionKey);
            }
            else {
                selectionKey.cancel();
            }
        }
    }

    public interface ChannelSelectedCallback {
        void onChannelSelected(SelectableChannel channel, SelectionKey selectionKey);
    }

    public void setReactorThreadCount(int reactorThreadCount) {
        if (reactorThreadCount < 2) {
            throw new IllegalArgumentException("Must have at least two reactor threads, but you requested " + reactorThreadCount);
        }

        synchronized (reactorThreads) {
            int deltaThreads = reactorThreadCount - reactorThreads.size();
            if (deltaThreads > 0) {
                // Start new reactor thread. Note that we start the threads before we increase the permits of the
                // actionsSemaphore.
                for (int i = 0; i < deltaThreads; i++) {
                    Reactor reactor = new Reactor();
                    reactor.setDaemon(true);
                    reactor.setName("Smack " + reactorName + " Thread #" + i);
                    reactorThreads.add(reactor);
                    reactor.start();
                }

                actionsSemaphore.release(deltaThreads);
            } else {
                // Stop existing reactor threads. First we change the sign of deltaThreads, then we decrease the permits
                // of the actionsSemaphore *before* we signal the selected reactor threads that they should shut down.
                deltaThreads -= deltaThreads;

                for (int i = deltaThreads - 1; i > 0; i--) {
                    // Note that this could potentially block forever, starving on the unfair semaphore.
                    actionsSemaphore.acquireUninterruptibly();
                }

                for (int i = deltaThreads - 1; i > 0; i--) {
                    Reactor reactor = reactorThreads.remove(i);
                    reactor.requestShutdown();
                }

                selector.wakeup();
            }
        }
    }

    public static final class SelectionKeyAttachment {
        private final WeakReference<ChannelSelectedCallback> weaeklyReferencedChannelSelectedCallback;
        private final AtomicBoolean reactorThreadRacing = new AtomicBoolean();

        private SelectionKeyAttachment(ChannelSelectedCallback channelSelectedCallback) {
            this.weaeklyReferencedChannelSelectedCallback = new WeakReference<>(channelSelectedCallback);
        }

        private void setRacing() {
            // We use lazySet here since it is sufficient if the value does not become visible immediately.
            reactorThreadRacing.lazySet(true);
        }

        public void resetReactorThreadRacing() {
            reactorThreadRacing.set(false);
        }

        public boolean isReactorThreadRacing() {
            return reactorThreadRacing.get();
        }

    }
}
