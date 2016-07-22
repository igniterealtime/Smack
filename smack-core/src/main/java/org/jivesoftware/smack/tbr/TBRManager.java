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
package org.jivesoftware.smack.tbr;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.tbr.element.TBRGetTokensIQ;
import org.jivesoftware.smack.tbr.element.TBRTokensIQ;

/**
 * Token-based reconnection manager class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://www.xmpp.org/extensions/inbox/token-reconnection.html">
 *      XEP-xxxx: Token-based reconnection</a>
 */
public final class TBRManager extends Manager {

    public static final String AUTH_NAMESPACE = "erlang-solutions.com:xmpp:token-auth:0";

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, TBRManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of TBRManager.
     *
     * @param connection
     * @return the instance of TBRManager
     */
    public static synchronized TBRManager getInstanceFor(XMPPConnection connection) {
        TBRManager tbrManager = INSTANCES.get(connection);

        if (tbrManager == null) {
            tbrManager = new TBRManager(connection);
            INSTANCES.put(connection, tbrManager);
        }

        return tbrManager;
    }

    private TBRManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * Get tokens.
     * 
     * @return the tokens
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public TBRTokens getTokens()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        TBRGetTokensIQ getTokensIQ = new TBRGetTokensIQ(connection().getUser().asBareJid());
        StanzaFilter responseFilter = new IQReplyFilter(getTokensIQ, connection());

        IQ responseIq = connection().createPacketCollectorAndSend(responseFilter, getTokensIQ).nextResultOrThrow();
        TBRTokensIQ tokensIQ = (TBRTokensIQ) responseIq;

        return new TBRTokens(tokensIQ.getAccessToken(), tokensIQ.getRefreshToken());
    }

}
