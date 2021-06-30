/**
 *
 * Copyright 2021 Guus der Kinderen
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
package org.jivesoftware.smack.subscription;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

/**
 * Integration tests that verify that sent presence subscription requests are received as intended.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class SubscriptionIntegrationTest extends AbstractSmackIntegrationTest {

    public SubscriptionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    /**
     * This test verifies that a subscription request is received.
     *
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest
    public void testSubscriptionRequest() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser())
                .build();

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        conOne.addAsyncStanzaListener(p -> received.signal(),
                stanza -> {
                    if (!(stanza instanceof Presence)) {
                        return false;
                    }
                    if (!stanza.getFrom().asBareJid().equals(conTwo.getUser().asBareJid())) {
                        return false;
                    }
                    final Presence presence = (Presence) stanza;
                    return Presence.Type.subscribe.equals(presence.getType());
                }
        );

        conTwo.sendStanza(subscriptionRequest);
        received.waitForResult(timeout);
    }

    /**
     * This test verifies that a subscription request is received, in a scenario where the intended recipient was
     * offline when the request was made.
     *
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest
    public void testSubscriptionRequestOffline() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        ((AbstractXMPPConnection) conOne).disconnect();

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser())
                .build();

        conTwo.sendStanza(subscriptionRequest);

        ((AbstractXMPPConnection) conOne).connect();

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        conOne.addAsyncStanzaListener(p -> received.signal(),
                stanza -> {
                    if (!(stanza instanceof Presence)) {
                        return false;
                    }
                    if (!stanza.getFrom().asBareJid().equals(conTwo.getUser().asBareJid())) {
                        return false;
                    }
                    final Presence presence = (Presence) stanza;
                    return Presence.Type.subscribe.equals(presence.getType());
                }
        );

        ((AbstractXMPPConnection) conOne).login();
        received.waitForResult(timeout);
    }

    /**
     * When a subscription request is made, the stanza can have additional extension elements. This test verifies that
     * such extension elements are received.
     *
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest
    public void testSubscriptionRequestWithExtension() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser())
                .addExtension(new StandardExtensionElement("test", "org.example.test"))
                .build();

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        conOne.addAsyncStanzaListener(p -> received.signal(),
                stanza -> {
                    if (!(stanza instanceof Presence)) {
                        return false;
                    }
                    if (!stanza.getFrom().asBareJid().equals(conTwo.getUser().asBareJid())) {
                        return false;
                    }
                    final Presence presence = (Presence) stanza;
                    if (!Presence.Type.subscribe.equals(presence.getType())) {
                        return false;
                    }
                    return stanza.hasExtension("test", "org.example.test");
                }
        );

        conTwo.sendStanza(subscriptionRequest);
        received.waitForResult(timeout);
    }

    /**
     * When a subscription request is made, the stanza can have additional extension elements. This test verifies that
     * such extension elements are received, in a scenario where the intended recipient was offline when the request
     * was made.
     *
     * @see <a href="https://igniterealtime.atlassian.net/browse/OF-2244">Openfire issue OF-2244</a>
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest
    public void testSubscriptionRequestOfflineWithExtension() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        ((AbstractXMPPConnection) conOne).disconnect();

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
                .ofType(Presence.Type.subscribe)
                .to(conOne.getUser())
                .addExtension(new StandardExtensionElement("test", "org.example.test"))
                .build();

        conTwo.sendStanza(subscriptionRequest);

        ((AbstractXMPPConnection) conOne).connect();

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        conOne.addAsyncStanzaListener(p -> received.signal(),
                stanza -> {
                    if (!(stanza instanceof Presence)) {
                        return false;
                    }
                    if (!stanza.getFrom().asBareJid().equals(conTwo.getUser().asBareJid())) {
                        return false;
                    }
                    final Presence presence = (Presence) stanza;
                    if (!Presence.Type.subscribe.equals(presence.getType())) {
                        return false;
                    }
                    return stanza.hasExtension("test", "org.example.test");
                }
        );

        ((AbstractXMPPConnection) conOne).login();
        received.waitForResult(timeout);
    }
}
