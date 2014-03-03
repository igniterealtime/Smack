/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smackx.time;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.time.packet.Time;

public class EntityTimeManager extends Manager {

    private static final Map<Connection, EntityTimeManager> INSTANCES = new WeakHashMap<Connection, EntityTimeManager>();

    private static final PacketFilter TIME_PACKET_FILTER = new AndFilter(new PacketTypeFilter(
                    Time.class), new IQTypeFilter(Type.GET));

    private static boolean autoEnable = true;

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                getInstanceFor(connection);
            }
        });
    }

    public static void setAutoEnable(boolean autoEnable) {
        EntityTimeManager.autoEnable = autoEnable;
    }

    public synchronized static EntityTimeManager getInstanceFor(Connection connection) {
        EntityTimeManager entityTimeManager = INSTANCES.get(connection);
        if (entityTimeManager == null) {
            entityTimeManager = new EntityTimeManager(connection);
        }
        return entityTimeManager;
    }

    private boolean enabled = false;

    private EntityTimeManager(Connection connection) {
        super(connection);
        INSTANCES.put(connection, this);
        if (autoEnable)
            enable();

        connection.addPacketListener(new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                if (!enabled)
                    return;
                connection().sendPacket(Time.createResponse(packet));
            }
        }, TIME_PACKET_FILTER);
    }

    public synchronized void enable() {
        if (enabled)
            return;
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        sdm.addFeature(Time.NAMESPACE);
        enabled = true;
    }

    public synchronized void disable() {
        if (!enabled)
            return;
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        sdm.removeFeature(Time.NAMESPACE);
        enabled = false;
    }

    public boolean isTimeSupported(String jid) throws XMPPException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, Time.NAMESPACE);
    }

    public Time getTime(String jid) throws XMPPException {
        if (!isTimeSupported(jid))
            return null;

        Time request = new Time();
        Time response = (Time) connection().createPacketCollectorAndSend(request).nextResultOrThrow();
        return response;
    }
}
