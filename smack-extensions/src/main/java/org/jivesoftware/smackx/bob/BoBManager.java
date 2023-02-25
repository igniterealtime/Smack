/**
 *
 * Copyright 2016-2021 Fernando Ramirez, Florian Schmaus
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

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
import org.jivesoftware.smack.util.SHA1;

import org.jivesoftware.smackx.bob.element.BoBIQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import org.jxmpp.jid.Jid;
import org.jxmpp.util.cache.LruCache;

/**
 * Bits of Binary manager class.
 *
 * @author Fernando Ramirez
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public final class BoBManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:bob";

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, BoBManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of BoBManager.
     *
     * @param connection TODO javadoc me please
     * @return the instance of BoBManager
     */
    public static synchronized BoBManager getInstanceFor(XMPPConnection connection) {
        BoBManager bobManager = INSTANCES.get(connection);
        if (bobManager == null) {
            bobManager = new BoBManager(connection);
            INSTANCES.put(connection, bobManager);
        }

        return bobManager;
    }

    private static final LruCache<ContentId, BoBData> BOB_CACHE = new LruCache<>(128);

    private final Map<ContentId, BoBInfo> bobs = new ConcurrentHashMap<>();

    private BoBManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        serviceDiscoveryManager.addFeature(NAMESPACE);

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(BoBIQ.ELEMENT, BoBIQ.NAMESPACE, IQ.Type.get, Mode.async) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        BoBIQ bobIQRequest = (BoBIQ) iqRequest;
                        ContentId contentId = bobIQRequest.getContentId();

                        BoBInfo bobInfo = bobs.get(contentId);
                        if (bobInfo == null) {
                            // TODO return item-not-found
                            return null;
                        }

                        BoBData bobData = bobInfo.getData();
                        BoBIQ responseBoBIQ = new BoBIQ(contentId, bobData);
                        responseBoBIQ.setType(IQ.Type.result);
                        responseBoBIQ.setTo(bobIQRequest.getFrom());
                        return responseBoBIQ;
                    }
                });
    }

    /**
     * Returns true if Bits of Binary is supported by the server.
     *
     * @return true if Bits of Binary is supported by the server.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(NAMESPACE);
    }

    /**
     * Request BoB data.
     *
     * @param to TODO javadoc me please
     * @param bobHash TODO javadoc me please
     * @return the BoB data
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public BoBData requestBoB(Jid to, ContentId bobHash) throws NotLoggedInException, NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        BoBData bobData = BOB_CACHE.lookup(bobHash);
        if (bobData != null) {
            return bobData;
        }

        BoBIQ requestBoBIQ = new BoBIQ(bobHash);
        requestBoBIQ.setType(IQ.Type.get);
        requestBoBIQ.setTo(to);

        XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        BoBIQ responseBoBIQ = connection.sendIqRequestAndWaitForResponse(requestBoBIQ);

        bobData = responseBoBIQ.getBoBData();
        BOB_CACHE.put(bobHash, bobData);

        return bobData;
    }

    public BoBInfo addBoB(BoBData bobData) {
        // We only support SHA-1 for now.
        ContentId bobHash = new ContentId(SHA1.hex(bobData.getContent()), "sha1");

        Set<ContentId> bobHashes = Collections.singleton(bobHash);
        bobHashes = Collections.unmodifiableSet(bobHashes);

        BoBInfo bobInfo = new BoBInfo(bobHashes, bobData);

        bobs.put(bobHash, bobInfo);

        return bobInfo;
    }

    public BoBInfo removeBoB(ContentId bobHash) {
        BoBInfo bobInfo = bobs.remove(bobHash);
        if (bobInfo == null) {
            return null;
        }
        for (ContentId otherBobHash : bobInfo.getHashes()) {
            bobs.remove(otherBobHash);
        }
        return bobInfo;
    }
}
