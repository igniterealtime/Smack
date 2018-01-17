/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.spoiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.spoiler.element.SpoilerElement;

public final class SpoilerManager extends Manager {

    public static final String NAMESPACE_0 = "urn:xmpp:spoiler:0";

    private static final Map<XMPPConnection, SpoilerManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Create a new SpoilerManager and add Spoiler to disco features.
     *
     * @param connection xmpp connection
     */
    private SpoilerManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE_0);
    }

    /**
     * Return the connections instance of the SpoilerManager.
     *
     * @param connection xmpp connection
     * @return SpoilerManager
     */
    public static SpoilerManager getInstanceFor(XMPPConnection connection) {
        SpoilerManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new SpoilerManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Create an empty SpoilerElement.
     *
     * @return empty SpoilerElement
     */
    public static SpoilerElement createSpoiler() {
        return SpoilerElement.EMPTY;
    }

    /**
     * Create a SpoilerElement with a hint about the content.
     *
     * @param hint hint about the spoiled content
     * @return SpoilerElement
     */
    public static SpoilerElement createSpoiler(String hint) {
        return new SpoilerElement(null, hint);
    }

    /**
     * Create a SpoilerElement with a hint about the content in a certain language.
     *
     * @param lang language of the hint (like en, es...)
     * @param hint hint about the spoiled content
     * @return SpoilerElement
     */
    public static SpoilerElement createSpoiler(String lang, String hint) {
        return new SpoilerElement(lang, hint);
    }


    /**
     * Returns true, if the message has at least one spoiler element.
     *
     * @param message message
     * @return true if message has spoiler extension
     */
    public static boolean containsSpoiler(Message message) {
        return message.hasExtension(SpoilerElement.ELEMENT, NAMESPACE_0);
    }

    /**
     * Return a map of all spoilers contained in a message.
     * The map uses the language of a spoiler as key.
     * If a spoiler has no language attribute, its key will be an empty String.
     *
     * @param message message
     * @return map of spoilers
     */
    public static Map<String, String> getSpoilers(Message message) {
        if (!containsSpoiler(message)) {
            return null;
        }

        List<ExtensionElement> spoilers = message.getExtensions(SpoilerElement.ELEMENT, NAMESPACE_0);
        Map<String, String> map = new HashMap<>();

        for (ExtensionElement e : spoilers) {
            SpoilerElement s = (SpoilerElement) e;
            if (s.getLanguage() == null || s.getLanguage().equals("")) {
                map.put("", s.getHint());
            } else {
                map.put(s.getLanguage(), s.getHint());
            }
        }

        return map;
    }
}
