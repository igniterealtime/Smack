/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smackx.pep;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.ExtensionElement;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint.ResultSyncPointTimeoutException;

public abstract class AbstractPepIntegrationTest extends AbstractSmackIntegrationTest {

    @FunctionalInterface
    public interface ThrowingAction {
        void run() throws NotLoggedInException, NotALeafNodeException, NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException;
    }

    protected final ServiceDiscoveryManager sdm1;
    protected final ServiceDiscoveryManager sdm2;
    protected final PingManager pingManager1;
    protected final PingManager pingManager2;

    protected AbstractPepIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        sdm1 = ServiceDiscoveryManager.getInstanceFor(conOne);
        sdm2 = ServiceDiscoveryManager.getInstanceFor(conTwo);
        pingManager1 = PingManager.getInstanceFor(conOne);
        pingManager2 = PingManager.getInstanceFor(conTwo);
    }

    @AfterClass
    public void unsubscribe() throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    /**
     * Registers a listener on conTwo and waits until the server has learned conTwo's updated CAPS features
     * and registered conTwo as an active subscriber on conOne's PEP node.
     *
     * @param <E> the type of element received by the listener
     * @param listenerRegisterer consumer that registers the listener on conTwo's manager
     * @param listener the PEP event listener to register
     * @throws NoResponseException if there was no response from the remote entity or server.
     * @throws XMPPErrorException if an XMPP error occurred.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    protected <E extends ExtensionElement> void registerListenerAndWait(
            Consumer<PepEventListener<E>> listenerRegisterer,
            PepEventListener<E> listener)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // 1. Register the listener on the client. This adds the +notify feature to Entity Capabilities and broadcasts an updated presence.
        listenerRegisterer.accept(listener);

        // 2. Query conTwo's disco info from conOne over the wire so the XMPP server (e.g., ejabberd's mod_caps)
        //    snoops the disco#info response and caches conTwo's new CAPS features including the +notify filter.
        sdm1.discoverInfo(conTwo.getUser(), null);

        // 3. Ping the server from conTwo to flush the stream. Because XMPP TCP streams are FIFO, receiving the
        //    ping response guarantees the server has processed conTwo's presence, disco response, and registered
        //    conTwo as an active subscriber on conOne's PEP node before any subsequent publication.
        pingManager2.pingMyServer();
    }

    /**
     * Publishes data using PEP from conOne, and blocks until the server has echoed the publication back to conOne.
     *
     * @param <E> the type of element received by the publication echo listener
     * @param echoListenerRegisterer consumer that registers the echo listener on conOne's manager
     * @param echoListenerUnregisterer consumer that removes the echo listener from conOne's manager
     * @param publishAction runnable that publishes the PEP item from conOne
     * @param echoFilter predicate testing whether the received PEP event matches the published data
     * @throws NotLoggedInException if the connection is not logged in.
     * @throws NotALeafNodeException if the PubSub node is not a leaf node.
     * @throws NoResponseException if there was no response from the remote entity or server.
     * @throws NotConnectedException if the connection is not connected.
     * @throws XMPPErrorException if an XMPP error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws ResultSyncPointTimeoutException if the publication echo was not received in time.
     */
    protected <E extends ExtensionElement> void publishAndWait(
            Consumer<PepEventListener<E>> echoListenerRegisterer,
            Consumer<PepEventListener<E>> echoListenerUnregisterer,
            ThrowingAction publishAction,
            Predicate<E> echoFilter)
            throws NotLoggedInException, NotALeafNodeException, NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, ResultSyncPointTimeoutException {
        final ResultSyncPoint<Boolean, ResultSyncPointTimeoutException> publicationEchoReceived = new ResultSyncPoint<>();
        final PepEventListener<E> publicationEchoListener = (jid, element, id, message) -> {
            if (echoFilter.test(element)) {
                publicationEchoReceived.signal(Boolean.TRUE);
            }
        };
        try {
            // 1. Register a listener on the publishing account to receive its own publication echo.
            echoListenerRegisterer.accept(publicationEchoListener);

            // 2. Query conOne's disco info from conTwo over the wire so the XMPP server (e.g., ejabberd's mod_caps)
            //    snoops the disco#info response and caches conOne's updated CAPS features (including the +notify filter).
            sdm2.discoverInfo(conOne.getUser(), null);

            // 3. Ping the server from conOne to flush the stream. Because XMPP TCP streams are FIFO, receiving the
            //    ping response guarantees the server has processed conOne's presence, disco response, and registered
            //    conOne to receive publication echos.
            pingManager1.pingMyServer();

            // 4. Publish the data and wait for the publication echo from the server.
            publishAction.run();
            publicationEchoReceived.waitForResult(timeout);
        } finally {
            echoListenerUnregisterer.accept(publicationEchoListener);
        }
    }
}
