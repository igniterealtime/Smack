/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.push_notifications;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.push_notifications.element.DisablePushNotificationsIQ;
import org.jivesoftware.smackx.push_notifications.element.EnablePushNotificationsIQ;
import org.jivesoftware.smackx.push_notifications.element.PushNotificationsElements;
import org.jxmpp.jid.Jid;

/**
 * Push Notifications manager class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0357.html">XEP-0357: Push
 *      Notifications</a>
 * @author Fernando Ramirez
 * 
 */
public final class PushNotificationsManager extends Manager {

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, PushNotificationsManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of PushNotificationsManager.
     *
     * @param connection
     * @return the instance of PushNotificationsManager
     */
    public static synchronized PushNotificationsManager getInstanceFor(XMPPConnection connection) {
        PushNotificationsManager pushNotificationsManager = INSTANCES.get(connection);

        if (pushNotificationsManager == null) {
            pushNotificationsManager = new PushNotificationsManager(connection);
            INSTANCES.put(connection, pushNotificationsManager);
        }

        return pushNotificationsManager;
    }

    private PushNotificationsManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * Returns true if Push Notifications is supported by the server.
     *
     * @return true if Push Notifications is supported by the server.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection())
                .serverSupportsFeature(PushNotificationsElements.NAMESPACE);
    }

    /**
     * Enable push notifications.
     * 
     * @param pushJid
     * @param node
     * @return true if it was successfully enabled, false if not
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean enable(Jid pushJid, String node)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return enable(pushJid, node, null);
    }

    /**
     * Enable push notifications.
     * 
     * @param pushJid
     * @param node
     * @param publishOptions
     * @return true if it was successfully enabled, false if not
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean enable(Jid pushJid, String node, HashMap<String, String> publishOptions)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        EnablePushNotificationsIQ enablePushNotificationsIQ = new EnablePushNotificationsIQ(pushJid, node,
                publishOptions);
        return changePushNotificationsStatus(enablePushNotificationsIQ);
    }

    /**
     * Disable all push notifications.
     * 
     * @param pushJid
     * @return true if it was successfully disabled, false if not
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean disableAll(Jid pushJid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return disable(pushJid, null);
    }

    /**
     * Disable push notifications of an specific node.
     * 
     * @param pushJid
     * @param node
     * @return true if it was successfully disabled, false if not
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean disable(Jid pushJid, String node)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DisablePushNotificationsIQ disablePushNotificationsIQ = new DisablePushNotificationsIQ(pushJid, node);
        return changePushNotificationsStatus(disablePushNotificationsIQ);
    }

    private boolean changePushNotificationsStatus(IQ iq)
            throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        final XMPPConnection connection = connection();
        IQ responseIQ = connection.createPacketCollectorAndSend(iq).nextResultOrThrow();
        return responseIQ.getType() != Type.error;
    }

}
