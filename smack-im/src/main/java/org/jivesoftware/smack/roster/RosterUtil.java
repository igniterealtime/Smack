/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smack.roster;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class RosterUtil {

    public static void waitUntilOtherEntityIsSubscribed(Roster roster, BareJid otherEntity, long timeoutMillis)
                    throws InterruptedException, TimeoutException {
        Date deadline = new Date(System.currentTimeMillis() + timeoutMillis);
        waitUntilOtherEntityIsSubscribed(roster, otherEntity, deadline);
    }

    public static void waitUntilOtherEntityIsSubscribed(Roster roster, final BareJid otherEntity, Date deadline)
                    throws InterruptedException, TimeoutException {
        final Lock lock = new ReentrantLock();
        final Condition maybeSubscribed = lock.newCondition();
        RosterListener rosterListener = new AbstractRosterListener() {
            private void signal() {
                lock.lock();
                try {
                    // No need to use signalAll() here.
                    maybeSubscribed.signal();
                }
                finally {
                    lock.unlock();
                }
            }

            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                signal();
            }

            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                signal();
            }
        };

        roster.addRosterListener(rosterListener);

        boolean stillWaiting = true;
        // Using the example code pattern from Condition.awaitUntil(Date) javadoc.
        lock.lock();
        try {
            while (!roster.isSubscribedToMyPresence(otherEntity)) {
                if (!stillWaiting) {
                    throw new TimeoutException();
                }
                stillWaiting = maybeSubscribed.awaitUntil(deadline);
            }
        }
        finally {
            lock.unlock();
            // Make sure the listener is removed, so we don't leak it.
            roster.removeRosterListener(rosterListener);
        }
    }

    public static void askForSubscriptionIfRequired(Roster roster, BareJid jid)
            throws NotLoggedInException, NotConnectedException, InterruptedException {
        RosterEntry entry = roster.getEntry(jid);
        if (entry == null || !(entry.canSeeHisPresence() || entry.isSubscriptionPending())) {
            roster.sendSubscriptionRequest(jid);
        }
    }

    public static void ensureNotSubscribedToEachOther(XMPPConnection connectionOne, XMPPConnection connectionTwo)
            throws NotConnectedException, InterruptedException {
        final Roster rosterOne = Roster.getInstanceFor(connectionOne);
        final BareJid jidOne = connectionOne.getUser().asBareJid();
        final Roster rosterTwo = Roster.getInstanceFor(connectionTwo);
        final BareJid jidTwo = connectionTwo.getUser().asBareJid();

        ensureNotSubscribed(rosterOne, jidTwo);
        ensureNotSubscribed(rosterTwo, jidOne);
    }

    public static void ensureNotSubscribed(Roster roster, BareJid jid)
            throws NotConnectedException, InterruptedException {
        RosterEntry entry = roster.getEntry(jid);
        if (entry != null && entry.canSeeMyPresence()) {
            entry.cancelSubscription();
        }
    }

    public static void ensureSubscribed(XMPPConnection connectionOne, XMPPConnection connectionTwo, long timeout)
                    throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        ensureSubscribedTo(connectionOne, connectionTwo, timeout);
        ensureSubscribedTo(connectionTwo, connectionOne, timeout);
    }

    public static void ensureSubscribedTo(XMPPConnection connectionOne, XMPPConnection connectionTwo, long timeout)
                    throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        Date deadline = new Date(System.currentTimeMillis() + timeout);
        ensureSubscribedTo(connectionOne, connectionTwo, deadline);
    }

    public static void ensureSubscribedTo(final XMPPConnection connectionOne, final XMPPConnection connectionTwo,
                    final Date deadline)
                    throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        final Roster rosterOne = Roster.getInstanceFor(connectionOne);
        final BareJid jidTwo = connectionTwo.getUser().asBareJid();

        if (rosterOne.iAmSubscribedTo(jidTwo))
            return;

        final BareJid jidOne = connectionOne.getUser().asBareJid();
        final SubscribeListener subscribeListener = new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                if (from.equals(jidOne)) {
                    return SubscribeAnswer.Approve;
                }
                return null;
            }
        };
        final Roster rosterTwo = Roster.getInstanceFor(connectionTwo);

        rosterTwo.addSubscribeListener(subscribeListener);
        try {
            rosterOne.sendSubscriptionRequest(jidTwo);
            waitUntilOtherEntityIsSubscribed(rosterTwo, jidOne, deadline);
        }
        finally {
            rosterTwo.removeSubscribeListener(subscribeListener);
        }
    }
}
