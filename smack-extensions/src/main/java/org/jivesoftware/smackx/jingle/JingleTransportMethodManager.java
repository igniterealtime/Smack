/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;

/**
 * Manager where TransportMethods are registered.
 */
public final class JingleTransportMethodManager extends Manager {

    private static final WeakHashMap<XMPPConnection, JingleTransportMethodManager> INSTANCES = new WeakHashMap<>();

    private final HashMap<String, JingleTransportManager<?>> transportManagers = new HashMap<>();

    private static final String[] transportPreference = new String[] {
            JingleS5BTransport.NAMESPACE_V1,
            JingleIBBTransport.NAMESPACE_V1
    };

    private JingleTransportMethodManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleTransportMethodManager getInstanceFor(XMPPConnection connection) {
        JingleTransportMethodManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleTransportMethodManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void registerTransportManager(JingleTransportManager<?> manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    public static JingleTransportManager<?> getTransportManager(XMPPConnection connection, String namespace) {
        return getInstanceFor(connection).getTransportManager(namespace);
    }

    public JingleTransportManager<?> getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    public static JingleTransportManager<?> getTransportManager(XMPPConnection connection, Jingle request) {
        return getInstanceFor(connection).getTransportManager(request);
    }
    public JingleTransportManager<?> getTransportManager(Jingle request) {

        JingleContent content = request.getContents().get(0);
        if (content == null) {
            return null;
        }

        JingleContentTransport transport = content.getTransport();
        if (transport == null) {
            return null;
        }

        return getTransportManager(transport.getNamespace());
    }

    public JingleTransportManager<?> getBestAvailableTransportManager(XMPPConnection connection) {
        return getInstanceFor(connection).getBestAvailableTransportManager();
    }

    public JingleTransportManager<?> getBestAvailableTransportManager() {
        JingleTransportManager<?> tm;
        for (String ns : transportPreference) {
            tm = getTransportManager(ns);
            if (tm != null) {
                return tm;
            }
        }

        Iterator<String> it = transportManagers.keySet().iterator();
        if (it.hasNext()) {
            return getTransportManager(it.next());
        }

        return null;
    }

    public JingleTransportManager<?> getBestAvailableTransportManager(Set<String> except) {
        JingleTransportManager<?> tm;
        for (String ns : transportPreference) {
            tm = getTransportManager(ns);
            if (tm != null) {
                if (except.contains(tm.getNamespace())) {
                    continue;
                }
                return tm;
            }
        }

        for (String ns : transportManagers.keySet()) {
            if (except.contains(ns)) {
                continue;
            }
            return getTransportManager(ns);
        }

        return null;
    }
}
