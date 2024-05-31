/**
 *
 * Copyright 2015-2024 Florian Schmaus, 2022-2024 Guus der Kinderen
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.roster.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

@SpecificationReference(document = "RFC6121")
public class RosterIntegrationTest extends AbstractSmackIntegrationTest {

    private final Roster rosterOne;
    private final Roster rosterTwo;

    public RosterIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        rosterOne = Roster.getInstanceFor(conOne);
        rosterTwo = Roster.getInstanceFor(conTwo);
    }

    @SmackIntegrationTest
    public void subscribeRequestListenerTest() throws TimeoutException, Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final SubscribeListener subscribeListener = new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                if (from.equals(conOne.getUser().asBareJid())) {
                    return SubscribeAnswer.Approve;
                }
                return SubscribeAnswer.Deny;
            }
        };
        rosterTwo.addSubscribeListener(subscribeListener);

        final String conTwosRosterName = "ConTwo " + testRunId;
        final SimpleResultSyncPoint addedAndSubscribed = new SimpleResultSyncPoint();
        final RosterListener rosterListener = new AbstractRosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                checkIfAddedAndSubscribed(addresses);
            }
            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                checkIfAddedAndSubscribed(addresses);
            }
            private void checkIfAddedAndSubscribed(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    if (!jid.equals(conTwo.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conTwo.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterOne.getEntry(bareJid);
                    if (rosterEntry == null) {
                        addedAndSubscribed.signalFailure("Added/Updated entry was not for " + bareJid);
                        return;
                    }
                    String name = rosterEntry.getName();
                    if (StringUtils.isNullOrEmpty(name)) {
                        addedAndSubscribed.signalFailure("Added/Updated entry without name");
                        return;
                    }
                    if (!rosterEntry.getName().equals(conTwosRosterName)) {
                        addedAndSubscribed.signalFailure("Added/Updated entry name does not match. Expected: " + conTwosRosterName + " but was: " + rosterEntry.getName());
                        return;
                    }
                    if (!rosterEntry.getType().equals(ItemType.to)) {
                        return;
                    }
                    addedAndSubscribed.signal();
                }
            }
        };

        rosterOne.addRosterListener(rosterListener);

        try {
            rosterOne.createItemAndRequestSubscription(conTwo.getUser().asBareJid(), conTwosRosterName, null);
            assertResult(addedAndSubscribed,
                "A roster entry for " + conTwo.getUser().asBareJid() + " using the name '" + conTwosRosterName +
                "' of type 'to' was expected to be added to the roster of " + conOne.getUser() + " (but it was not).");
        }
        finally {
            rosterTwo.removeSubscribeListener(subscribeListener);
            rosterOne.removeRosterListener(rosterListener);
        }
    }

    /**
     * Asserts that when a user sends out a presence subscription request, the server sends a roster push back to the
     * user.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.2", quote =
        "After locally delivering or remotely routing the presence subscription request, the user's server MUST then " +
        "send a roster push to all of the user's interested resources, containing the potential contact with a " +
        "subscription state of \"none\" and with notation that the subscription is pending (via an 'ask' attribute " +
        "whose value is \"subscribe\").")
    public void testRosterPushAfterSubscriptionRequest() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.manual); // prevents a race condition when asserting the captured roster entry.
        final ResultSyncPoint<RosterEntry, Exception> added = new ResultSyncPoint<>();

        final RosterListener rosterListener = new AbstractRosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    if (!jid.equals(conTwo.getUser().asBareJid())) {
                        continue;
                    }
                    final BareJid bareJid = conTwo.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterOne.getEntry(bareJid);
                    added.signal(rosterEntry);
                    return;
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        final Presence subscribe = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            final RosterEntry rosterEntry = assertResult(added, "Expected the server to send a roster push back to '" + conOne.getUser() + "' after they sent a presence subscription request to '" + conTwo.getUser().asBareJid() + "' (but the server did not).");
            assertEquals(ItemType.none, rosterEntry.getType(), "Unexpected subscription type on roster push after '" + conOne.getUser() + "' sent a presence subscription request to '" + conTwo.getUser().asBareJid() + "'.");
            assertTrue(rosterEntry.isSubscriptionPending(), "Missing 'ask=subscribe' attribute on roster push after '" + conOne.getUser() + "' sent a presence subscription request to '" + conTwo.getUser().asBareJid() + "'.");
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterOne.removeRosterListener(rosterListener);
        }
    }

    /**
     * Asserts that when a user sends out a presence subscription request to an entity for which the user does not have
     * a pre-existing subscription, the server will deliver the subscription request to that entity.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.3", quote =
        "if there is at least one available resource associated with the contact when the subscription request is " +
        "received by the contact's server, then the contact's server MUST send that subscription request to all " +
        "available resources in accordance with Section 8.")
    public void testPresenceDeliveredToRecipient() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final ResultSyncPoint<Presence, Exception> added = new ResultSyncPoint<>();
        final StanzaListener stanzaListener = stanza -> added.signal((Presence) stanza);
        conTwo.addAsyncStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(conOne.getUser())));

        final Presence subscribe = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);
            final Presence received = assertResult(added, "Expected subscription request from '" + conOne.getUser() + "' to '" + conTwo.getUser().asBareJid() + "' to be delivered to " + conTwo.getUser() + " (but it did not).");
            assertEquals(Presence.Type.subscribe, received.getType(), "Unexpected presence type in presence stanza received by '" + conTwo.getUser() + "' after '" + conOne.getUser() + "' sent a presence subscription request.");
        } finally {
            conTwo.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server stamps the bare JID of the sender,
     * and delivers it to the requester.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.5", quote =
        "When the contact's client sends the subscription approval, the contact's server MUST stamp the outbound " +
        "stanza with the bare JID <contact@domainpart> of the contact and locally deliver or remotely route the " +
        "stanza to the user.")
    public void testPresenceApprovalStampedAndDelivered() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        // Modify the outbound 'subscribed' stanza, to be 'wrong' (addressed to a full rather than a bare JID), to test if the server overrides this.
        final Consumer<PresenceBuilder> interceptor = (PresenceBuilder presenceBuilder) -> presenceBuilder.to(conOne.getUser()).build();
        conTwo.addPresenceInterceptor(interceptor, p -> p.getType() == Presence.Type.subscribed);

        final ResultSyncPoint<Presence, Exception> added = new ResultSyncPoint<>();
        final StanzaListener stanzaListener = stanza -> added.signal((Presence) stanza);

        conOne.addAsyncStanzaListener(stanzaListener, PresenceTypeFilter.SUBSCRIBED);

        final Presence subscribe = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            final Presence received = assertResult(added, "Expected presence 'subscribed' stanza to be delivered to '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' approved their subscription request (but it was not).");
            assertEquals(conTwo.getUser().asBareJid(), received.getFrom().asEntityBareJidOrThrow(), "Expected presence 'subscribed' stanza that was delivered to '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' approved their subscription request to have a 'from' attribute value that is associated to '" + conTwo.getUser().getLocalpart() + "' (but it did not).");
            assertTrue(received.getFrom().isEntityBareJid(), "Expected presence 'subscribed' stanza that was delivered to '" + conOne.getUser() + "' after '" + conTwo.getUser() + "' approved their subscription request to have a 'from' attribute value that is a bare JID (but it was not).");
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            conTwo.removePresenceInterceptor(interceptor);
            conOne.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server sends a roster push to the user with
     * a subscription 'from'.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.5", quote =
        "The contact's server then MUST send an updated roster push to all of the contact's interested resources, " +
        "with the 'subscription' attribute set to a value of \"from\".")
    public void testPresenceApprovalYieldsRosterPush() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final ResultSyncPoint<RosterEntry, Exception> updated = new ResultSyncPoint<>();

        final RosterListener rosterListener = new AbstractRosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    if (!jid.equals(conOne.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conOne.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterTwo.getEntry(bareJid);
                    updated.signal(rosterEntry);
                }
            }
        };
        rosterTwo.addRosterListener(rosterListener);

        final Presence subscribe = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);
            // The 'subscribe' gets automatically approved by conTwo.

            final RosterEntry entry = assertResult(updated, "Expected '" + conTwo.getUser() + "' to receive a roster push with an update for the entry of '" + conOne.getUser().asBareJid() + "' after '" + conTwo.getUser() + "' approved their subscription request.");
            assertEquals(ItemType.from, entry.getType(), "Unexpected type for '" + conOne.getUser().asBareJid() + "''s entry in '" + conTwo.getUser().asBareJid() + "''s roster.");
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server sends a roster push to the user with
     * a subscription 'both' when the contact already has a subscription to the other entity.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.5", quote =
        "The contact's server then MUST send an updated roster push to all of the contact's interested resources, " +
        "with the 'subscription' attribute set to a value of \"from\". (Here we assume that the contact does not " +
        "already have a subscription to the user; if that were the case, the 'subscription' attribute would be set " +
        "to a value of \"both\", as explained under Appendix A.)")
    public void testPresenceApprovalYieldsRosterPush2() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        // Setup fixture: establish one-way subscription.
        rosterOne.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint fixtureComplete = new SimpleResultSyncPoint();
        RosterListener rosterListenerTwo = new AbstractRosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                checkIfAdded(addresses);
            }
            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                checkIfAdded(addresses);
            }
            private void checkIfAdded(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    final BareJid bareJid = conOne.getUser().asBareJid();
                    if (!jid.equals(bareJid)) {
                        continue;
                    }
                    if (rosterTwo.getEntry(bareJid) == null) {
                        continue;
                    }
                    if (rosterTwo.getEntry(bareJid).getType() == ItemType.none) {
                        continue;
                    }
                    fixtureComplete.signal();
                    rosterTwo.removeRosterListener(this);
                }
            }
        };
        rosterTwo.addRosterListener(rosterListenerTwo);

        final Presence subscribeOne = conTwo.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser().asBareJid())
                .build();
        try {
            conTwo.sendStanza(subscribeOne);

            fixtureComplete.waitForResult(connection.getReplyTimeout());
        } finally {
            rosterOne.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListenerTwo);
        }

        // Setup fixture is now complete. Execute the test.
        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final ResultSyncPoint<RosterEntry, Exception> updated = new ResultSyncPoint<>();

        rosterListenerTwo = new AbstractRosterListener() {
            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    if (!jid.equals(conOne.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conOne.getUser().asBareJid();
                    updated.signal(rosterTwo.getEntry(bareJid));
                }
            }
        };
        rosterTwo.addRosterListener(rosterListenerTwo);

        final Presence subscribeTwo = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribeTwo);

            final RosterEntry entry = assertResult(updated, "Expected '" + conTwo.getUser() + "' to receive a roster push with an update for the entry of '" + conOne.getUser().asBareJid() + "' after '" + conOne.getUser() + "' approved their subscription request.");
            assertEquals(ItemType.both, entry.getType(), "Unexpected type for '" + conOne.getUser().asBareJid() + "''s entry in '" + conTwo.getUser().asBareJid() + "''s roster.");
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListenerTwo);
        }
    }

    /**
     * Asserts that when a presence subscription request is approved, the server sends the latest presence of the now
     * subscribed entity to the subscriber.
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest(section = "3.1.5", quote =
        "The contact's server MUST then also send current presence to the user from each of the contact's available resources.")
    public void testCurrentPresenceSentAfterSubscriptionApproval() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final String needle = "Look for me!";
        conTwo.sendStanza(conTwo.getStanzaFactory().buildPresenceStanza().setStatus(needle).build());

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = stanza -> {
            final Presence presence = (Presence) stanza;

            String status = presence.getStatus();
            if (status == null) return;

            if (status.equals(needle)) {
                received.signal();
            }
        };
        conOne.addAsyncStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(conTwo.getUser())));

        final Presence subscribe = conOne.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertResult(received, "Expected '" + conTwo.getUser() + "' to receive '" + conOne.getUser() + "''s current presence update (including the status '" + needle + "'), but it did not.");
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            conOne.removeAsyncStanzaListener(stanzaListener);
        }
    }

}
