/**
 *
 * Copyright 2016-2017 Fernando Ramirez, 2016-2020 Florian Schmaus
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
package org.jivesoftware.smackx.blocking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.blocking.element.BlockContactsIQ;
import org.jivesoftware.smackx.blocking.element.BlockListIQ;
import org.jivesoftware.smackx.blocking.element.UnblockContactsIQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.jid.Jid;

/**
 * Blocking command manager class.
 *
 * @author Fernando Ramirez
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public final class BlockingCommandManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:blocking";

    private volatile List<Jid> blockListCached;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, BlockingCommandManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of BlockingCommandManager.
     *
     * @param connection TODO javadoc me please
     * @return the instance of BlockingCommandManager
     */
    public static synchronized BlockingCommandManager getInstanceFor(XMPPConnection connection) {
        BlockingCommandManager blockingCommandManager = INSTANCES.get(connection);

        if (blockingCommandManager == null) {
            blockingCommandManager = new BlockingCommandManager(connection);
            INSTANCES.put(connection, blockingCommandManager);
        }

        return blockingCommandManager;
    }

    private final Set<AllJidsUnblockedListener> allJidsUnblockedListeners = new CopyOnWriteArraySet<>();

    private final Set<JidsBlockedListener> jidsBlockedListeners = new CopyOnWriteArraySet<>();

    private final Set<JidsUnblockedListener> jidsUnblockedListeners = new CopyOnWriteArraySet<>();

    private BlockingCommandManager(XMPPConnection connection) {
        super(connection);

        // block IQ handler
        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(BlockContactsIQ.ELEMENT, BlockContactsIQ.NAMESPACE, IQ.Type.set, Mode.sync) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        BlockContactsIQ blockContactIQ = (BlockContactsIQ) iqRequest;

                        if (blockListCached == null) {
                            blockListCached = new ArrayList<>();
                        }

                        List<Jid> blockedJids = blockContactIQ.getJids();
                        blockListCached.addAll(blockedJids);

                        for (JidsBlockedListener listener : jidsBlockedListeners) {
                            listener.onJidsBlocked(blockedJids);
                        }

                        return IQ.createResultIQ(blockContactIQ);
                    }
                });

        // unblock IQ handler
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(UnblockContactsIQ.ELEMENT,
                UnblockContactsIQ.NAMESPACE, IQ.Type.set, Mode.sync) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                UnblockContactsIQ unblockContactIQ = (UnblockContactsIQ) iqRequest;

                if (blockListCached == null) {
                    blockListCached = new ArrayList<>();
                }

                List<Jid> unblockedJids = unblockContactIQ.getJids();
                if (unblockedJids == null) { // remove all
                    blockListCached.clear();
                    for (AllJidsUnblockedListener listener : allJidsUnblockedListeners) {
                        listener.onAllJidsUnblocked();
                    }
                } else { // remove only some
                    blockListCached.removeAll(unblockedJids);
                    for (JidsUnblockedListener listener : jidsUnblockedListeners) {
                        listener.onJidsUnblocked(unblockedJids);
                    }
                }

                return IQ.createResultIQ(unblockContactIQ);
            }
        });

        connection.addConnectionListener(new ConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                // No need to reset the cache if the connection got resumed.
                if (resumed) {
                    return;
                }
                blockListCached = null;
            }
        });
    }

    /**
     * Returns true if Blocking Command is supported by the server.
     *
     * @return true if Blocking Command is supported by the server.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(NAMESPACE);
    }

    /**
     * Returns the block list.
     *
     * @return the blocking list
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Jid> getBlockList()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        if (blockListCached == null) {
            BlockListIQ blockListIQ = new BlockListIQ();
            BlockListIQ blockListIQResult = connection().createStanzaCollectorAndSend(blockListIQ).nextResultOrThrow();
            blockListCached = blockListIQResult.getBlockedJidsCopy();
        }

        return Collections.unmodifiableList(blockListCached);
    }

    /**
     * Block contacts.
     *
     * @param jids TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void blockContacts(List<Jid> jids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        BlockContactsIQ blockContactIQ = new BlockContactsIQ(jids);
        connection().createStanzaCollectorAndSend(blockContactIQ).nextResultOrThrow();
    }

    /**
     * Unblock contacts.
     *
     * @param jids TODO javadoc me please
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void unblockContacts(List<Jid> jids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        UnblockContactsIQ unblockContactIQ = new UnblockContactsIQ(jids);
        connection().createStanzaCollectorAndSend(unblockContactIQ).nextResultOrThrow();
    }

    /**
     * Unblock all.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void unblockAll()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        UnblockContactsIQ unblockContactIQ = new UnblockContactsIQ();
        connection().createStanzaCollectorAndSend(unblockContactIQ).nextResultOrThrow();
    }

    public void addJidsBlockedListener(JidsBlockedListener jidsBlockedListener) {
        jidsBlockedListeners.add(jidsBlockedListener);
    }

    public void removeJidsBlockedListener(JidsBlockedListener jidsBlockedListener) {
        jidsBlockedListeners.remove(jidsBlockedListener);
    }

    public void addJidsUnblockedListener(JidsUnblockedListener jidsUnblockedListener) {
        jidsUnblockedListeners.add(jidsUnblockedListener);
    }

    public void removeJidsUnblockedListener(JidsUnblockedListener jidsUnblockedListener) {
        jidsUnblockedListeners.remove(jidsUnblockedListener);
    }

    public void addAllJidsUnblockedListener(AllJidsUnblockedListener allJidsUnblockedListener) {
        allJidsUnblockedListeners.add(allJidsUnblockedListener);
    }

    public void removeAllJidsUnblockedListener(AllJidsUnblockedListener allJidsUnblockedListener) {
        allJidsUnblockedListeners.remove(allJidsUnblockedListener);
    }
}
