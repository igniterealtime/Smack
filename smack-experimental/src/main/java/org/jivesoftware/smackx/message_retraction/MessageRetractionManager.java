/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.message_retraction;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.MessageBuilder;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.message_fastening.element.FasteningElement;
import org.jivesoftware.smackx.message_retraction.element.RetractElement;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

/**
 * Smacks API for XEP-0424: Message Retraction.
 *
 * To enable / disable auto-announcing support for this feature, call {@link #setEnabledByDefault(boolean)}.
 * Auto-announcing is enabled by default.
 *
 * To retract a message, call {@link #retractMessage(OriginIdElement)}, passing in the {@link OriginIdElement Origin ID}
 * of the message to be retracted.
 */
public final class MessageRetractionManager extends Manager {

    private static final Map<XMPPConnection, MessageRetractionManager> INSTANCES = new WeakHashMap<>();

    private static boolean ENABLED_BY_DEFAULT = false;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (ENABLED_BY_DEFAULT) {
                    getInstanceFor(connection).announceSupport();
                }
            }
        });
    }

    private MessageRetractionManager(XMPPConnection connection) {
        super(connection);
    }

    public static synchronized MessageRetractionManager getInstanceFor(XMPPConnection connection) {
        MessageRetractionManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new MessageRetractionManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Enable or disable auto-announcing support for Message Retraction.
     * Default is disabled.
     *
     * @param enabled enabled
     */
    public static synchronized void setEnabledByDefault(boolean enabled) {
        ENABLED_BY_DEFAULT = enabled;
    }

    /**
     * Announce support for Message Retraction to the server.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0424.html#disco">XEP-0424: Message Retraction: §2. Discovering Support</a>
     */
    public void announceSupport() {
        ServiceDiscoveryManager.getInstanceFor(connection()).addFeature(RetractElement.NAMESPACE);
    }

    /**
     * Stop announcing support for Message Retraction.
     */
    public void stopAnnouncingSupport() {
        ServiceDiscoveryManager.getInstanceFor(connection()).removeFeature(RetractElement.NAMESPACE);
    }

    /**
     * Append a {@link RetractElement} wrapped inside a {@link FasteningElement} which contains
     * the {@link OriginIdElement Origin-ID} of the message that will be retracted to the given {@link MessageBuilder}.
     *
     * @param retractedMessageId {@link OriginIdElement OriginID} of the message that the user wants to retract
     * @param carrierMessageBuilder message used to transmit the message retraction to the recipient
     */
    public static void addRetractionElementToMessage(OriginIdElement retractedMessageId, MessageBuilder carrierMessageBuilder) {
        FasteningElement fasteningElement = FasteningElement.builder()
                .setOriginId(retractedMessageId)
                .addWrappedPayload(new RetractElement())
                .build();
        fasteningElement.applyTo(carrierMessageBuilder);
    }

    /**
     * Retract a message by appending a {@link RetractElement} wrapped inside a {@link FasteningElement} which contains
     * the {@link OriginIdElement Origin-ID} of the message that will be retracted to a new message and send it to the
     * server.
     *
     * @param retractedMessageId {@link OriginIdElement OriginID} of the message that the user wants to retract
     * @throws SmackException.NotConnectedException in case the connection is not connected.
     * @throws InterruptedException if the thread gets interrupted.
     */
    public void retractMessage(OriginIdElement retractedMessageId)
            throws SmackException.NotConnectedException, InterruptedException {
        MessageBuilder message = connection().getStanzaFactory().buildMessageStanza();
        addRetractionElementToMessage(retractedMessageId, message);
        connection().sendStanza(message.build());
    }
}
