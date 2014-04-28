/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

/**
 * Manager for HTTP ove XMPP transport (XEP-0332) extension.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HOXTManager {

    /**
     * Namespace for this extension.
     */
    public static final String NAMESPACE = "urn:xmpp:http";

    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE);
            }
        });
    }

    /**
     * Returns true if the given entity understands the HTTP ove XMPP transport format and allows the exchange of such.
     *
     * @param jid jid
     * @param connection connection
     * @return true if the given entity understands the HTTP ove XMPP transport format and exchange.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     */
    public static boolean isSupported(String jid, XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException {
        return ServiceDiscoveryManager.getInstanceFor(connection).supportsFeature(jid, NAMESPACE);
    }
}
