/**
 *
 * Copyright 2015-2020 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
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
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

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

        final SubscribeListener subscribeListener = (from, subscribeRequest) -> {
            if (from.equals(conOne.getUser().asBareJid())) {
                return SubscribeListener.SubscribeAnswer.Approve;
            }
            return SubscribeListener.SubscribeAnswer.Deny;
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
                        addedAndSubscribed.signalFailure("No roster entry for " + bareJid);
                        return;
                    }
                    String name = rosterEntry.getName();
                    if (StringUtils.isNullOrEmpty(name)) {
                        addedAndSubscribed.signalFailure("Roster entry without name");
                        return;
                    }
                    if (!rosterEntry.getName().equals(conTwosRosterName)) {
                        addedAndSubscribed.signalFailure("Roster name does not match");
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

            assertTrue(addedAndSubscribed.waitForResult(2 * connection.getReplyTimeout()));
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
     * <p>From RFC6121 § 3.1.2:</p>
     * <blockquote>
     * After locally delivering or remotely routing the presence subscription request, the user's server MUST then send
     * a roster push to all of the user's interested resources, containing the potential contact with a subscription
     * state of "none" and with notation that the subscription is pending (via an 'ask' attribute whose value is
     * "subscribe").
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testRosterPushAfterSubscriptionRequest() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();
        final RosterListener rosterListener = new AbstractRosterListener() {
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
                    if (!jid.equals(conTwo.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conTwo.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterOne.getEntry(bareJid);
                    if (rosterEntry == null) {
                        added.signalFailure("No roster entry for " + bareJid);
                        return;
                    }

                    if (rosterEntry.getType() != ItemType.none) {
                        added.signalFailure("Incorrect subscription type on roster entry: " + rosterEntry.getType());
                        return;
                    }

                    if (!rosterEntry.isSubscriptionPending()) {
                        added.signalFailure("No 'ask' on roster entry.");
                        return;
                    }

                    added.signal();
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            rosterOne.removeRosterListener(rosterListener);
        }
    }

    /**
     * Asserts that when a user sends out a presence subscription request to an entity for which the user already has
     * an approved subscription, the server sends an auto-reply back to the user.
     *
     * <p>From RFC6121 § 3.1.3:</p>
     * <blockquote>
     * If the contact exists and the user already has a subscription to the contact's presence, then the contact's
     * server MUST auto-reply on behalf of the contact by sending a presence stanza of type "subscribed" from the
     * contact's bare JID to the user's bare JID.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testAutoReplyForRequestWhenAlreadySubscribed() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, connection.getReplyTimeout());

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = stanza -> {
            final Presence presence = (Presence) stanza;
            if (!presence.getTo().isEntityBareJid()) {
                added.signalFailure("'to' address should be a bare JID, but is a full JID.");
            } else if (!presence.getFrom().isEntityBareJid()) {
                added.signalFailure("'from' address should be a bare JID, but is a full JID.");
            } else if (presence.getType() != Presence.Type.subscribed) {
                added.signalFailure("Incorrect subscription type on auto-reply: " + presence.getType());
            } else {
                added.signal();
            }
        };

        conOne.addAsyncStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(conTwo.getUser())));

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            conOne.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user sends out a presence subscription request to an entity for which the user does not have
     * a pre-existing subscription, the server will deliver the subscription request to that entity.
     *
     * <p>From RFC6121 § 3.1.3:</p>
     * <blockquote>
     * if there is at least one available resource associated with the contact when the subscription request is received
     * by the contact's server, then the contact's server MUST send that subscription request to all available resources
     * in accordance with Section 8.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testPresenceDeliveredToRecipient() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = stanza -> {
            final Presence presence = (Presence) stanza;
            if (presence.getType() != Presence.Type.subscribe) {
                added.signalFailure("Incorrect subscription type on auto-reply: " + presence.getType());
            } else {
                added.signal();
            }
        };

        conTwo.addAsyncStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(conOne.getUser())));

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            conTwo.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server stamps the bare JID of the sender,
     * and delivers it to the requester.
     *
     * <p>From RFC6121 § 3.1.5:</p>
     * <blockquote>
     * When the contact's client sends the subscription approval, the contact's server MUST stamp the outbound stanza
     * with the bare JID &lt;contact@domainpart&gt; of the contact and locally deliver or remotely route the stanza to the
     * user.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testPresenceApprovalStampedAndDelivered() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        // Modify the outbound 'subscribed' stanza, to be 'wrong' (addressed to a full rather than a bare JID), to test if the server overrides this.
        final Consumer<PresenceBuilder> interceptor = (PresenceBuilder presenceBuilder) -> presenceBuilder.to(conOne.getUser()).build();
        conTwo.addPresenceInterceptor(interceptor, p -> p.getType() == Presence.Type.subscribed);

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = stanza -> {
            final Presence presence = (Presence) stanza;
            if (!presence.getFrom().isEntityBareJid()) {
                added.signalFailure("'from' address should be a bare JID, but is a full JID.");
            } else {
                added.signal();
            }
        };

        conOne.addAsyncStanzaListener(stanzaListener, new AndFilter(PresenceTypeFilter.SUBSCRIBED, FromMatchesFilter.createBare(conTwo.getUser())));

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            conTwo.removePresenceInterceptor(interceptor);
            conOne.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server sends a roster push to the user with
     * a subscription 'from'
     *
     * <p>From RFC6121 § 3.1.5:</p>
     * <blockquote>
     * The contact's server then MUST send an updated roster push to all of the contact's interested resources, with the
     * 'subscription' attribute set to a value of "from".
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testPresenceApprovalYieldsRosterPush() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();

        final RosterListener rosterListener = new AbstractRosterListener() {
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
                    if (!jid.equals(conOne.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conOne.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterTwo.getEntry(bareJid);
                    if (rosterEntry == null) {
                        added.signalFailure("No roster entry for " + bareJid);
                        return;
                    }
                    if (!rosterEntry.getType().equals(ItemType.from)) {
                        added.signalFailure("Incorrect roster entry type. Expected 'from', got: " + rosterEntry.getType());
                        return;
                    }
                    added.signal();
                }
            }
        };
        rosterTwo.addRosterListener(rosterListener);

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListener);
        }
    }

    /**
     * Asserts that when a user sends a presence subscription approval, the server sends a roster push to the user with
     * a subscription 'both' when the contact already has a subscription to the other entity.
     *
     * <p>From RFC6121 § 3.1.5:</p>
     * <blockquote>
     * The contact's server then MUST send an updated roster push to all of the contact's interested resources, with the
     * 'subscription' attribute set to a value of "from". (Here we assume that the contact does not already have a
     * subscription to the user; if that were the case, the 'subscription' attribute would be set to a value of "both",
     * as explained under Appendix A.)
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
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

        final Presence subscribeOne = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser().asBareJid())
                .build();
        try {
            conTwo.sendStanza(subscribeOne);

            fixtureComplete.waitForResult(2 * connection.getReplyTimeout());
        } finally {
            rosterOne.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListenerTwo);
        }

        // Setup fixture is now complete. Execute the test.
        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint added = new SimpleResultSyncPoint();

        rosterListenerTwo = new AbstractRosterListener() {
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
                    if (!jid.equals(conOne.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conOne.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterTwo.getEntry(bareJid);
                    if (rosterEntry == null) {
                        added.signalFailure("No roster entry for " + bareJid);
                        return;
                    }
                    if (!rosterEntry.getType().equals(ItemType.both)) {
                        added.signalFailure("Incorrect roster entry type. Expected 'both', got: " + rosterEntry.getType());
                        return;
                    }
                    added.signal();
                }
            }
        };
        rosterTwo.addRosterListener(rosterListenerTwo);

        final Presence subscribeTwo = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribeTwo);

            assertTrue(added.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterTwo.removeRosterListener(rosterListenerTwo);
        }
    }

    /**
     * Asserts that when a presence subscription request is approved, the server sends the latest presence of the now
     * subscribed entity to the subscriber.
     *
     * <p>From RFC6121 § 3.1.5:</p>
     * <blockquote>
     * The contact's server MUST then also send current presence to the user from each of the contact's available
     * resources.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testCurrentPresenceSentAfterSubscriptionApproval() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final String needle = "Look for me!";
        conTwo.sendStanza(PresenceBuilder.buildPresence().setStatus(needle).build());

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = stanza -> {
            final Presence presence = (Presence) stanza;
            if (presence.getStatus().equals(needle)) {
                received.signal();
            }
        };
        conOne.addAsyncStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, FromMatchesFilter.createBare(conTwo.getUser())));

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);

            assertTrue(received.waitForResult(2 * connection.getReplyTimeout()));
        } finally {
            rosterTwo.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            conOne.removeAsyncStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that when a user receives a presence subscription approval, the server first sends the presence stanza,
     * followed by a roster push.
     *
     * <p>From RFC6121 § 3.1.6:</p>
     * <blockquote>
     * (...)  If this check is successful, then the user's server MUST:
     * 1. Deliver the inbound subscription approval to all of the user's interested resources (...). This MUST occur
     *    before sending the roster push described in the next step.
     * 2. Initiate a roster push to all of the user's interested resources, containing an updated roster item for the
     *    contact with the 'subscription' attribute set to a value of "to" (...) or "both" (...).
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void testReceivePresenceApprovalAndRosterPush() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        rosterTwo.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        final SimpleResultSyncPoint receivedPresence = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint receivedRosterPush = new SimpleResultSyncPoint();

        final StanzaListener stanzaListener = stanza -> {
            try {
                receivedRosterPush.waitForResult(0);
                // roster push should NOT have been received yet. If it has, fail the test.
                receivedPresence.signalFailure("Received roster push before subscription approval.");
            } catch (TimeoutException e) {
                // Expected
                receivedPresence.signal();
            } catch (Exception e) {
                receivedPresence.signalFailure("Unexpected exception: " + e.getMessage());
            }
        };

        // Add as a synchronous listener, as this test asserts the order of stanzas.
        conOne.addSyncStanzaListener(stanzaListener, new AndFilter(PresenceTypeFilter.SUBSCRIBED, FromMatchesFilter.createBare(conTwo.getUser())));

        final RosterListener rosterListener = new AbstractRosterListener() {
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
                    if (!jid.equals(conTwo.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conTwo.getUser().asBareJid();
                    if (!List.of(ItemType.to, ItemType.both).contains(rosterOne.getEntry(bareJid).getType())) {
                        receivedRosterPush.signalFailure("Incorrect roster entry type. Expected 'to' or 'both'");
                        return;
                    }
                    try {
                        receivedPresence.waitForResult(0);
                        receivedRosterPush.signal();
                    } catch (TimeoutException e) {
                        // subscription approval should have been received by now. If not, fail the test.
                        receivedRosterPush.signalFailure("Received roster push before subscription approval.");
                    } catch (Exception e) {
                        receivedRosterPush.signalFailure("Unexpected exception: " + e.getMessage());
                    }
                }
            }
        };
        rosterOne.addRosterListener(rosterListener);

        final Presence subscribe = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.subscribe)
                .to(conTwo.getUser().asBareJid())
                .build();

        try {
            conOne.sendStanza(subscribe);
            assertTrue(receivedPresence.waitForResult(2 * connection.getReplyTimeout()));
            assertTrue(receivedRosterPush.waitForResult(connection.getReplyTimeout()));
        } finally {
            rosterOne.setSubscriptionMode(Roster.getDefaultSubscriptionMode());
            rosterOne.removeRosterListener(rosterListener);
            conOne.removeSyncStanzaListener(stanzaListener);
        }
    }
}
