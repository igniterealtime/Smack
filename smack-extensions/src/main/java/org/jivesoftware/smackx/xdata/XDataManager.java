/**
 *
 * Copyright 2014-2015 Florian Schmaus
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
package org.jivesoftware.smackx.xdata;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

public final class XDataManager extends Manager {

    /**
     * The value of {@link DataForm#NAMESPACE}.
     */
    public static final String NAMESPACE = DataForm.NAMESPACE;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, XDataManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the XDataManager for the given XMPP connection.
     *
     * @param connection the XMPPConnection.
     * @return the XDataManager
     */
    public static synchronized XDataManager getInstanceFor(XMPPConnection connection) {
        XDataManager xDataManager = INSTANCES.get(connection);
        if (xDataManager == null) {
            xDataManager = new XDataManager(connection);
            INSTANCES.put(connection, xDataManager);
        }
        return xDataManager;
    }

    private XDataManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        serviceDiscoveryManager.addFeature(NAMESPACE);
    }

    /**
     * Check if the given entity supports data forms.
     *
     * @param jid the JID of the entity to check.
     * @return true if the entity supports data forms.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @see <a href="http://xmpp.org/extensions/xep-0004.html#disco">XEP-0004: Data Forms § 6. Service Discovery</a>
     * @since 4.1
     */
    public boolean isSupported(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, NAMESPACE);
    }
}
