/*
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
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.PresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

public class IntegrationTestRosterUtil {

    public static void ensureBothAccountsAreSubscribedToEachOther(XMPPConnection conOne, XMPPConnection conTwo, long timeout)
            throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        ensureSubscribedTo(conOne, conTwo, timeout);
        ensureSubscribedTo(conTwo, conOne, timeout);
    }

    public static void ensureSubscribedTo(final XMPPConnection presenceRequestReceiverConnection, final XMPPConnection presenceRequestingConnection, long timeout)
            throws NotLoggedInException, NotConnectedException, InterruptedException, TimeoutException {
        final Roster presenceRequestReceiverRoster = Roster.getInstanceFor(presenceRequestReceiverConnection);
        final Roster presenceRequestingRoster = Roster.getInstanceFor(presenceRequestingConnection);

        final EntityFullJid presenceRequestReceiverAddress = presenceRequestReceiverConnection.getUser();
        final EntityFullJid presenceRequestingAddress = presenceRequestingConnection.getUser();

        if (presenceRequestReceiverRoster.isSubscribedToMyPresence(presenceRequestingAddress)
                && presenceRequestingRoster.iAmSubscribedTo(presenceRequestReceiverAddress)) {
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

        final ResultSyncPoint<Boolean, ResultSyncPoint.ResultSyncPointTimeoutException> syncPoint = new ResultSyncPoint<>();
        final PresenceEventListener presenceEventListener = new AbstractPresenceEventListener() {
            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                if (!address.equals(presenceRequestReceiverAddress.asBareJid())) {
                    return;
                }
                syncPoint.signal(Boolean.TRUE);
            }
        };
        presenceRequestingRoster.addPresenceEventListener(presenceEventListener);

        try {
            presenceRequestingRoster.sendSubscriptionRequest(presenceRequestReceiverAddress.asBareJid());

            syncPoint.waitForResult(timeout, "Timeout while waiting for subscription request of '" + presenceRequestingAddress + "' to '" + presenceRequestReceiverAddress + "' to be answered.");
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
        BareJid c2BareJid = c2.getUser().asBareJid();

        // 1. Send 'unsubscribed' presence to cancel/deny any pending inbound subscription request
        Presence unsubscribed = c1.getStanzaFactory().buildPresenceStanza()
                .to(c2BareJid)
                .ofType(Presence.Type.unsubscribed)
                .build();
        c1.sendStanza(unsubscribed);

        // 2. Send roster remove IQ to delete the item from the server's roster
        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.set);
        RosterPacket.Item item = new RosterPacket.Item(c2BareJid, null);
        item.setItemType(RosterPacket.ItemType.remove);
        packet.addRosterItem(item);

        try {
            c1.sendIqRequestAndWaitForResponse(packet);
        } catch (XMPPErrorException e) {
            // Account for race conditions: server-sided, the item might already have been removed or never existed.
            if (e.getStanzaError().getCondition() == StanzaError.Condition.item_not_found) {
                return;
            }
            throw e;
        }
    }

}
