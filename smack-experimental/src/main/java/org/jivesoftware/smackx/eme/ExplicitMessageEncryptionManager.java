/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.eme;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;

public final class ExplicitMessageEncryptionManager {

    private static final Map<XMPPConnection, ExplicitMessageEncryptionManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    public static final String NAMESPACE_V0 = ExplicitMessageEncryptionElement.NAMESPACE;

    public static synchronized ExplicitMessageEncryptionManager getInstanceFor(XMPPConnection connection) {
        ExplicitMessageEncryptionManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new ExplicitMessageEncryptionManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private ExplicitMessageEncryptionManager(XMPPConnection connection) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE_V0);
    }

}
