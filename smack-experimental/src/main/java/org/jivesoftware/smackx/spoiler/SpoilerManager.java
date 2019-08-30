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

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

public final class SpoilerManager extends Manager {

    public static final String NAMESPACE_0 = "urn:xmpp:spoiler:0";

    private static final Map<XMPPConnection, SpoilerManager> INSTANCES = new WeakHashMap<>();

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    /**
     * Create a new SpoilerManager and add Spoiler to disco features.
     *
     * @param connection xmpp connection
     */
    private SpoilerManager(XMPPConnection connection) {
        super(connection);
        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    /**
     * Begin announcing support for Spoiler messages.
     */
    public void startAnnounceSupport() {
        serviceDiscoveryManager.addFeature(NAMESPACE_0);
    }

    /**
     * End announcing support for Spoiler messages.
     */
    public void stopAnnounceSupport() {
        serviceDiscoveryManager.removeFeature(NAMESPACE_0);
    }

    /**
     * Return the connections instance of the SpoilerManager.
     *
     * @param connection xmpp connection
     * @return SpoilerManager TODO javadoc me please
     */
    public static synchronized SpoilerManager getInstanceFor(XMPPConnection connection) {
        SpoilerManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new SpoilerManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }
}
