/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.bob;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

/**
 * Bits of Binary manager class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public final class BoBManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:bob";
    public static BoBSaverManager bobSaverManager;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection, bobSaverManager);
            }
        });
    }

    private static final Map<XMPPConnection, BoBManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of BoBManager.
     * 
     * @param connection
     * @param saverManager
     * @return the instance of BoBManager
     */
    public static synchronized BoBManager getInstanceFor(XMPPConnection connection, BoBSaverManager saverManager) {
        if (saverManager == null) {
            bobSaverManager = new DefaultBoBSaverManager();
        } else {
            bobSaverManager = saverManager;
        }

        BoBManager bobManager = INSTANCES.get(connection);
        if (bobManager == null) {
            bobManager = new BoBManager(connection);
            INSTANCES.put(connection, bobManager);
        }

        return bobManager;
    }

    private BoBManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        serviceDiscoveryManager.addFeature(NAMESPACE);

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(BoBIQ.ELEMENT, BoBIQ.NAMESPACE, Type.get, Mode.sync) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        BoBIQ getBoBIQ = (BoBIQ) iqRequest;

                        BoBData bobData = bobSaverManager.getBoB(getBoBIQ.getBoBHash());
                        BoBIQ responseBoBIQ = null;
                        try {
                            responseBoBIQ = responseBoB(getBoBIQ, bobData);
                        } catch (NotConnectedException | NotLoggedInException | InterruptedException e) {
                        }

                        return responseBoBIQ;
                    }
                });

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(BoBIQ.ELEMENT, BoBIQ.NAMESPACE, Type.result, Mode.sync) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        BoBIQ resultBoBIQ = (BoBIQ) iqRequest;
                        bobSaverManager.addBoB(resultBoBIQ.getBoBHash(), resultBoBIQ.getBoBData());
                        return null;
                    }
                });
    }

    /**
     * Returns true if Bits of Binary is supported by the server.
     * 
     * @return true if Bits of Binary is supported by the server.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(NAMESPACE);
    }

    /**
     * Request BoB data.
     * 
     * @param to
     * @param bobHash
     * @return the BoB data
     * @throws NotLoggedInException
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public BoBData requestBoB(Jid to, BoBHash bobHash) throws NotLoggedInException, NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        BoBIQ requestBoBIQ = new BoBIQ(bobHash);
        requestBoBIQ.setType(Type.get);
        requestBoBIQ.setTo(to);

        XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        BoBIQ responseBoBIQ = connection.createPacketCollectorAndSend(requestBoBIQ).nextResultOrThrow();
        return responseBoBIQ.getBoBData();
    }

    private BoBIQ responseBoB(BoBIQ requestBoBIQ, BoBData bobData)
            throws NotConnectedException, InterruptedException, NotLoggedInException {
        BoBIQ responseBoBIQ = new BoBIQ(requestBoBIQ.getBoBHash(), bobData);
        responseBoBIQ.setType(Type.result);
        responseBoBIQ.setTo(requestBoBIQ.getFrom());
        return responseBoBIQ;
    }

}
