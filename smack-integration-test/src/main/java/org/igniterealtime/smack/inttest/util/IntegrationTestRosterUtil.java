/**
 *
 * Copyright 2015-2019 Florian Schmaus
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
package org.igniterealtime.smack.inttest.util;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.PresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.SubscribeListener;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

public class IntegrationTestRosterUtil {

    public static void ensureBothAccountsAreSubscribedToEachOther(XMPPConnection conOne, XMPPConnection conTwo, long timeout) throws TimeoutException, Exception {
        ensureSubscribedTo(conOne, conTwo, timeout);
        ensureSubscribedTo(conTwo, conOne, timeout);
    }

    public static void ensureSubscribedTo(final XMPPConnection presenceRequestReceiverConnection, final XMPPConnection presenceRequestingConnection, long timeout) throws TimeoutException, Exception {
        final Roster presenceRequestReceiverRoster = Roster.getInstanceFor(presenceRequestReceiverConnection);
        final Roster presenceRequestingRoster = Roster.getInstanceFor(presenceRequestingConnection);

        final EntityFullJid presenceRequestReceiverAddress = presenceRequestReceiverConnection.getUser();
        final EntityFullJid presenceRequestingAddress = presenceRequestingConnection.getUser();

        if (presenceRequestReceiverRoster.isSubscribedToMyPresence(presenceRequestingAddress)) {
            return;
        }

        final SubscribeListener subscribeListener = new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                if (from.equals(presenceRequestingConnection.getUser().asBareJid())) {
                    return SubscribeAnswer.Approve;
                }
                return SubscribeAnswer.Deny;
            }
        };
        presenceRequestReceiverRoster.addSubscribeListener(subscribeListener);

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
        final PresenceEventListener presenceEventListener = new AbstractPresenceEventListener() {
            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                if (!address.equals(presenceRequestReceiverAddress.asBareJid())) {
                    return;
                }
                syncPoint.signal();
            }
        };
        presenceRequestingRoster.addPresenceEventListener(presenceEventListener);

        try {
            presenceRequestingRoster.sendSubscriptionRequest(presenceRequestReceiverAddress.asBareJid());

            syncPoint.waitForResult(timeout);
        } finally {
            presenceRequestReceiverRoster.removeSubscribeListener(subscribeListener);
            presenceRequestingRoster.removePresenceEventListener(presenceEventListener);
        }
    }

    public static void ensureBothAccountsAreNotInEachOthersRoster(XMPPConnection conOne, XMPPConnection conTwo)
            throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException,
            InterruptedException {
        notInRoster(conOne, conTwo);
        notInRoster(conTwo, conOne);
    }

    private static void notInRoster(XMPPConnection c1, XMPPConnection c2) throws NotLoggedInException,
            NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Roster roster = Roster.getInstanceFor(c1);
        RosterEntry c2Entry = roster.getEntry(c2.getUser().asBareJid());
        if (c2Entry == null) {
            return;
        }
        roster.removeEntry(c2Entry);
    }

}
