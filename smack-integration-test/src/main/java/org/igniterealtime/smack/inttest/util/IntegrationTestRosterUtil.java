/**
 *
 * Copyright 2015-2018 Florian Schmaus
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
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.SubscribeListener;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class IntegrationTestRosterUtil {

    public static void ensureBothAccountsAreSubscribedToEachOther(XMPPConnection conOne, XMPPConnection conTwo, long timeout) throws TimeoutException, Exception {
        ensureSubscribedTo(conOne, conTwo, timeout);
        ensureSubscribedTo(conTwo, conOne, timeout);
    }

    public static void ensureSubscribedTo(final XMPPConnection conOne, final XMPPConnection conTwo, long timeout) throws TimeoutException, Exception {
        Roster rosterOne = Roster.getInstanceFor(conOne);
        Roster rosterTwo = Roster.getInstanceFor(conTwo);

        if (rosterOne.isSubscribedToMyPresence(conTwo.getUser())) {
            return;
        }

        final SubscribeListener subscribeListener = new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                if (from.equals(conTwo.getUser().asBareJid())) {
                    return SubscribeAnswer.Approve;
                }
                return SubscribeAnswer.Deny;
            }
        };
        rosterOne.addSubscribeListener(subscribeListener);

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
        rosterTwo.addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                if (!address.equals(conOne.getUser().asBareJid())) {
                    return;
                }
                syncPoint.signal();
            }
        });
        rosterTwo.sendSubscriptionRequest(conOne.getUser().asBareJid());

        try {
            syncPoint.waitForResult(timeout);
        } finally {
            rosterOne.removeSubscribeListener(subscribeListener);
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
