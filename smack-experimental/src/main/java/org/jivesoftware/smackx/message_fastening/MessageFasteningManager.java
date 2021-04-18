/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.message_fastening;

import java.util.List;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.MessageBuilder;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.message_fastening.element.FasteningElement;

/**
 * Smacks API for XEP-0422: Message Fastening.
 * The API is still very bare bones, as the XEP intends Message Fastening to be used as a tool by other protocols.
 *
 * To enable / disable auto-announcing support for this feature, call {@link #setEnabledByDefault(boolean)} (default true).
 *
 * To fasten a payload to a previous message, create an {@link FasteningElement} using the builder provided by
 * {@link FasteningElement#builder()}.
 *
 * You need to provide the {@link org.jivesoftware.smackx.sid.element.OriginIdElement} of the message you want to reference.
 * Then add wrapped payloads using {@link FasteningElement.Builder#addWrappedPayloads(List)}
 * and external payloads using {@link FasteningElement.Builder#addExternalPayloads(List)}.
 *
 * If you fastened some payloads onto the message previously and now want to replace the previous fastening, call
 * {@link FasteningElement.Builder#isRemovingElement()}.
 * Once you are finished, build the {@link FasteningElement} using {@link FasteningElement.Builder#build()} and add it to
 * a stanza by calling {@link FasteningElement#applyTo(MessageBuilder)}.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0422.html">XEP-0422: Message Fastening</a>
 */
public final class MessageFasteningManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:fasten:0";

    private static boolean ENABLED_BY_DEFAULT = false;

    private static final WeakHashMap<XMPPConnection, MessageFasteningManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (ENABLED_BY_DEFAULT) {
                    MessageFasteningManager.getInstanceFor(connection).announceSupport();
                }
            }
        });
    }

    private MessageFasteningManager(XMPPConnection connection) {
        super(connection);
    }

    public static synchronized MessageFasteningManager getInstanceFor(XMPPConnection connection) {
        MessageFasteningManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new MessageFasteningManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Enable or disable auto-announcing support for Message Fastening.
     * Default is enabled.
     *
     * @param enabled enabled
     */
    public static synchronized void setEnabledByDefault(boolean enabled) {
        ENABLED_BY_DEFAULT = enabled;
    }

    /**
     * Announce support for Message Fastening via Service Discovery.
     */
    public void announceSupport() {
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection());
        discoveryManager.addFeature(NAMESPACE);
    }

    /**
     * Stop announcing support for Message Fastening.
     */
    public void stopAnnouncingSupport() {
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection());
        discoveryManager.removeFeature(NAMESPACE);
    }
}
