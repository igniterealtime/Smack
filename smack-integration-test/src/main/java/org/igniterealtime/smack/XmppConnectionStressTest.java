/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
package org.igniterealtime.smack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.BooleansUtils;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;

import org.igniterealtime.smack.XmppConnectionStressTest.StressTestFailedException.ErrorsWhileSendingOrReceivingException;
import org.igniterealtime.smack.XmppConnectionStressTest.StressTestFailedException.NotAllMessagesReceivedException;
import org.jxmpp.jid.EntityFullJid;

public class XmppConnectionStressTest {

    private static final String MESSAGE_NUMBER_PROPERTY = "message-number";

    public static class Configuration {
        public final long seed;
        public final int messagesPerConnection;
        public final int maxPayloadChunkSize;
        public final int maxPayloadChunks;
        public final boolean intermixMessages;

        public Configuration(long seed, int messagesPerConnection, int maxPayloadChunkSize, int maxPayloadChunks,
                boolean intermixMessages) {
            this.seed = seed;
            this.messagesPerConnection = messagesPerConnection;
            this.maxPayloadChunkSize = maxPayloadChunkSize;
            this.maxPayloadChunks = maxPayloadChunks;
            this.intermixMessages = intermixMessages;
        }
    }

    private final Configuration configuration;

    public XmppConnectionStressTest(Configuration configuration) {
        this.configuration = configuration;
    }

    private volatile long waitStart;

    public void run(List<? extends XMPPConnection> connections, final long replyTimeoutMillis)
            throws InterruptedException, NotAllMessagesReceivedException, ErrorsWhileSendingOrReceivingException {
        final MultiMap<XMPPConnection, Message> messages = new MultiMap<>();
        final Random random = new Random(configuration.seed);
        final Map<XMPPConnection, Exception> sendExceptions = new ConcurrentHashMap<>();
        final Map<XMPPConnection, Exception> receiveExceptions = new ConcurrentHashMap<>();

        waitStart = -1;

        for (XMPPConnection fromConnection : connections) {
            MultiMap<XMPPConnection, Message> toConnectionMessages = new MultiMap<>();
            for (XMPPConnection toConnection : connections) {
                for (int i = 0; i < configuration.messagesPerConnection; i++) {
                    Message message = new Message();
                    message.setTo(toConnection.getUser());

                    int payloadChunkCount = random.nextInt(configuration.maxPayloadChunks) + 1;
                    for (int c = 0; c < payloadChunkCount; c++) {
                        int payloadChunkSize = random.nextInt(configuration.maxPayloadChunkSize) + 1;
                        String payloadCunk = StringUtils.randomString(payloadChunkSize, random);
                        JivePropertiesManager.addProperty(message, "payload-chunk-" + c, payloadCunk);
                    }

                    JivePropertiesManager.addProperty(message, MESSAGE_NUMBER_PROPERTY, i);

                    toConnectionMessages.put(toConnection, message);
                }
            }

            if (configuration.intermixMessages) {
                while (!toConnectionMessages.isEmpty()) {
                    int next = random.nextInt(connections.size());
                    Message message = null;
                    while (message == null) {
                        XMPPConnection toConnection = connections.get(next);
                        message = toConnectionMessages.getFirst(toConnection);
                        next = (next + 1) % connections.size();
                    }
                    messages.put(fromConnection, message);
                }
            } else {
                for (XMPPConnection toConnection : connections) {
                    for (Message message : toConnectionMessages.getAll(toConnection)) {
                        messages.put(fromConnection, message);
                    }
                }
            }
        }

        Semaphore receivedSemaphore = new Semaphore(-connections.size() + 1);
        Map<XMPPConnection, Map<EntityFullJid, boolean[]>> receiveMarkers = new ConcurrentHashMap<>(connections.size());

        for (XMPPConnection connection : connections) {
            connection.addSyncStanzaListener(new StanzaListener() {
                @Override
                public void processStanza(Stanza stanza) {
                    waitStart = System.currentTimeMillis();

                    EntityFullJid from = stanza.getFrom().asEntityFullJidOrThrow();
                    Message message = (Message) stanza;
                    JivePropertiesExtension extension = JivePropertiesExtension.from(message);

                    Integer messageNumber = (Integer) extension.getProperty(MESSAGE_NUMBER_PROPERTY);

                    Map<EntityFullJid, boolean[]> myReceiveMarkers = receiveMarkers.get(connection);
                    if (myReceiveMarkers == null) {
                        myReceiveMarkers = new HashMap<>(connections.size());
                        receiveMarkers.put(connection, myReceiveMarkers);
                    }

                    boolean[] fromMarkers = myReceiveMarkers.get(from);
                    if (fromMarkers == null) {
                        fromMarkers = new boolean[configuration.messagesPerConnection];
                        myReceiveMarkers.put(from, fromMarkers);
                    }

                    // Sanity check: All markers before must be true, all markers including the messageNumber marker must be false.
                    for (int i = 0; i < fromMarkers.length; i++) {
                        if ((i < messageNumber && !fromMarkers[i])
                                || (i >= messageNumber && fromMarkers[i])) {
                            // TODO: Better exception.
                            Exception exception = new Exception("out of order");
                            receiveExceptions.put(connection, exception);
                            // TODO: Current Smack design does not guarantee that the listener won't be invoked again.
                            // This is because the decission to invoke a sync listeners is done at a different place
                            // then invoking the listener.
                            connection.removeSyncStanzaListener(this);
                            receivedSemaphore.release();
                            return;
                        }
                    }

                    fromMarkers[messageNumber] = true;

                    if (myReceiveMarkers.size() != connections.size()) {
                        return;
                    }

                    for (boolean[] markers : myReceiveMarkers.values()) {
                        if (BooleansUtils.contains(markers, false)) {
                            return;
                        }
                    }
                    // All markers set to true, this means we received all messages.
                    receivedSemaphore.release();
                }
            }, new AndFilter(MessageTypeFilter.NORMAL,
                    new StanzaExtensionFilter(JivePropertiesExtension.ELEMENT, JivePropertiesExtension.NAMESPACE)));
        }

        Semaphore sendSemaphore = new Semaphore(-connections.size() + 1);

        for (XMPPConnection connection : connections) {
            Async.go(() -> {
                List<Message> messagesToSend;
                synchronized (messages) {
                    messagesToSend = messages.getAll(connection);
                }
                try {
                    for (Message messageToSend : messagesToSend) {
                        connection.sendStanza(messageToSend);
                    }
                } catch (NotConnectedException | InterruptedException e) {
                    sendExceptions.put(connection, e);
                } finally {
                    sendSemaphore.release();
                }
            });
        }

        sendSemaphore.acquire();

        if (waitStart < 0) {
            waitStart = System.currentTimeMillis();
        }

        boolean acquired;
        do {
            long acquireWait = waitStart + replyTimeoutMillis - System.currentTimeMillis();
            acquired = receivedSemaphore.tryAcquire(acquireWait, TimeUnit.MILLISECONDS);
        } while (!acquired && System.currentTimeMillis() < waitStart + replyTimeoutMillis);

        if (!acquired && receiveExceptions.isEmpty() && sendExceptions.isEmpty()) {
            throw new StressTestFailedException.NotAllMessagesReceivedException(receiveMarkers, connections);
        }

        if (!receiveExceptions.isEmpty() || !sendExceptions.isEmpty()) {
            throw new StressTestFailedException.ErrorsWhileSendingOrReceivingException(sendExceptions,
                    receiveExceptions);
        }

        // Test successful.
    }

    public abstract static class StressTestFailedException extends Exception {

        private static final long serialVersionUID = 1L;

        protected StressTestFailedException(String message) {
            super(message);
        }

        public static final class NotAllMessagesReceivedException extends StressTestFailedException {

            private static final long serialVersionUID = 1L;

            public final Map<XMPPConnection, Map<EntityFullJid, boolean[]>> receiveMarkers;

            private NotAllMessagesReceivedException(Map<XMPPConnection, Map<EntityFullJid, boolean[]>> receiveMarkers, List<? extends XMPPConnection> connections) {
                super("Did not receive all messages\n" + markersToString(receiveMarkers, connections).toString());
                this.receiveMarkers = receiveMarkers;
            }

            public static StringBuilder markersToString(Map<XMPPConnection, Map<EntityFullJid, boolean[]>> receiveMarkers, List<? extends XMPPConnection> connections) {
                StringBuilder sb = new StringBuilder();
                final int connectionCount = connections.size();

                Map<EntityFullJid, Integer> connectionIds = new HashMap<>(connectionCount);
                for (int i = 0; i < connectionCount; i++) {
                    XMPPConnection connection = connections.get(i);
                    EntityFullJid connectionAddress = connection.getUser();
                    connectionIds.put(connectionAddress, i);
                }

                for (Map.Entry<XMPPConnection, Map<EntityFullJid, boolean[]>> entry : receiveMarkers.entrySet()) {
                    XMPPConnection connection = entry.getKey();
                    Map<EntityFullJid, boolean[]> receiveMarkersOfThisConnection = entry.getValue();
                    Integer markerToConnectionId = connectionIds.get(connection.getUser());

                    for (Map.Entry<EntityFullJid, boolean[]> receiveMarkerOfThisConnection : receiveMarkersOfThisConnection.entrySet()) {
                        boolean[] marker = receiveMarkerOfThisConnection.getValue();
                        int numberOfFalseMarkers = BooleansUtils.numberOf(marker, false);
                        if (numberOfFalseMarkers == 0) {
                            continue;
                        }

                        EntityFullJid markerFromAddress = receiveMarkerOfThisConnection.getKey();
                        Integer markerFromConnectionId = connectionIds.get(markerFromAddress);
                        sb.append(markerToConnectionId)
                        .append(" is missing ").append(numberOfFalseMarkers)
                        .append(" messages from ").append(markerFromConnectionId)
                        .append(" :");
                        for (int i = 0; i < marker.length; i++) {
                            if (marker[i]) {
                                continue;
                            }
                            sb.append(i).append(", ");
                        }
                        sb.setLength(sb.length() - 2);
                        sb.append('\n');
                    }
                }

                return sb;
            }
        }

        public static final class ErrorsWhileSendingOrReceivingException extends StressTestFailedException {

            private static final long serialVersionUID = 1L;

            public final Map<XMPPConnection, Exception> sendExceptions;
            public final Map<XMPPConnection, Exception> receiveExceptions;

            private ErrorsWhileSendingOrReceivingException(Map<XMPPConnection, Exception> sendExceptions,
                    Map<XMPPConnection, Exception> receiveExceptions) {
                super("Exceptions while sending and/or receiving");
                this.sendExceptions = sendExceptions;
                this.receiveExceptions = receiveExceptions;
            }
        }
    }
}
