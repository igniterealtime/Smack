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
package org.jivesoftware.smackx.vcardtemp;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

public class VCardManager {
    public static final String NAMESPACE = "vcard-temp";
    public static final String ELEMENT = "vCard";

    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
            }
        });
    }

    /**
     * Returns true if the given entity understands the vCard-XML format and allows the exchange of such.
     * 
     * @param jid
     * @param connection
     * @return true if the given entity understands the vCard-XML format and exchange.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public static boolean isSupported(String jid, XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException  {
        return ServiceDiscoveryManager.getInstanceFor(connection).supportsFeature(jid, NAMESPACE);
    }
}
