/**
 *
 * Copyright 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.amp;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.amp.packet.AMPExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

/**
 * Manages AMP stanzas within messages. A AMPManager provides a high level access to
 * get and set AMP rules to messages.
 *
 * See http://xmpp.org/extensions/xep-0079.html for AMP extension details
 *
 * @author Vyacheslav Blinov
 */
public class AMPManager {


    // Enable the AMP support on every established connection
    // The ServiceDiscoveryManager class should have been already initialized
    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                AMPManager.setServiceEnabled(connection, true);
            }
        });
    }

    /**
     * Enables or disables the AMP support on a given connection.<p>
     *
     * Before starting to send AMP messages to a user, check that the user can handle XHTML
     * messages. Enable the AMP support to indicate that this client handles XHTML messages.
     *
     * @param connection the connection where the service will be enabled or disabled
     * @param enabled indicates if the service will be enabled or disabled
     */
    public synchronized static void setServiceEnabled(XMPPConnection connection, boolean enabled) {
        if (isServiceEnabled(connection) == enabled)
            return;

        if (enabled) {
            ServiceDiscoveryManager.getInstanceFor(connection).addFeature(AMPExtension.NAMESPACE);
        }
        else {
            ServiceDiscoveryManager.getInstanceFor(connection).removeFeature(AMPExtension.NAMESPACE);
        }
    }

    /**
     * Returns true if the AMP support is enabled for the given connection.
     *
     * @param connection the connection to look for AMP support
     * @return a boolean indicating if the AMP support is enabled for the given connection
     */
    public static boolean isServiceEnabled(XMPPConnection connection) {
        connection.getServiceName();
        return ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(AMPExtension.NAMESPACE);
    }

    /**
     * Check if server supports specified action
     * @param connection active xmpp connection
     * @param action action to check
     * @return true if this action is supported.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public static boolean isActionSupported(XMPPConnection connection, AMPExtension.Action action) throws NoResponseException, XMPPErrorException, NotConnectedException {
        String featureName = AMPExtension.NAMESPACE + "?action=" + action.toString();
        return isFeatureSupportedByServer(connection, featureName, AMPExtension.NAMESPACE);
    }

    /**
     * Check if server supports specified condition
     * @param connection active xmpp connection
     * @param conditionName name of condition to check
     * @return true if this condition is supported.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @see AMPDeliverCondition
     * @see AMPExpireAtCondition
     * @see AMPMatchResourceCondition
     */
    public static boolean isConditionSupported(XMPPConnection connection, String conditionName) throws NoResponseException, XMPPErrorException, NotConnectedException {
        String featureName = AMPExtension.NAMESPACE + "?condition=" + conditionName;
        return isFeatureSupportedByServer(connection, featureName, AMPExtension.NAMESPACE);
    }

    private static boolean isFeatureSupportedByServer(XMPPConnection connection, String featureName, String node) throws NoResponseException, XMPPErrorException, NotConnectedException {
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        DiscoverInfo info = discoveryManager.discoverInfo(connection.getServiceName(), node);
        for (DiscoverInfo.Feature feature : info.getFeatures()){
            if (featureName.equals(feature.getVar())) {
                return true;
            }
        }
        return false;
    }
}
