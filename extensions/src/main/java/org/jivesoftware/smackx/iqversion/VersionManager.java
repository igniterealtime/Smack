/**
 *
 * Copyright 2014 Georg Lukas.
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

package org.jivesoftware.smackx.iqversion;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.iqversion.packet.Version;

/**
 * A Version Manager that automatically responds to version IQs with a predetermined reply.
 *
 * <p>
 * The VersionManager takes care of handling incoming version request IQs, according to
 * XEP-0092 (Software Version). You can configure the version reply for a given connection
 * by running the following code:
 * </p>
 *
 * <pre>
 * Version MY_VERSION = new Version("My Little XMPP Application", "v1.23", "OS/2 32-bit");
 * VersionManager.getInstanceFor(mConnection).setVersion(MY_VERSION);
 * </pre>
 *
 * @author Georg Lukas
 */
public class VersionManager {
    private static final Map<Connection, VersionManager> instances =
            Collections.synchronizedMap(new WeakHashMap<Connection, VersionManager>());

    private Version own_version;
    private WeakReference<Connection> weakRefConnection;

    private VersionManager(final Connection connection) {
        this.weakRefConnection = new WeakReference<Connection>(connection);
        instances.put(connection, this);

        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(Version.NAMESPACE);

        connection.addPacketListener(new PacketListener() {
            /**
             * Sends a Version reply on request
             */
            public void processPacket(Packet packet) {
                if (own_version == null)
                    return;

                Version reply = new Version(own_version);
                reply.setPacketID(packet.getPacketID());
                reply.setTo(packet.getFrom());
                weakRefConnection.get().sendPacket(reply);
            }
        }
        , new AndFilter(new PacketTypeFilter(Version.class), new IQTypeFilter(Type.GET)));
    }

    public static synchronized VersionManager getInstanceFor(Connection connection) {
        VersionManager versionManager = instances.get(connection);

        if (versionManager == null) {
            versionManager = new VersionManager(connection);
        }

        return versionManager;
    }

    public void setVersion(Version v) {
        own_version = v;
    }
}
