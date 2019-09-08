/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.jid_prep;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jid_prep.element.JidPrepIq;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

public class JidPrepManager extends Manager {

    public static final String NAMESPACE = JidPrepIq.NAMESPACE;

    private static final Map<XMPPConnection, JidPrepManager> INSTANCES = new WeakHashMap<>();

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    public static synchronized JidPrepManager getInstanceFor(XMPPConnection connection) {
        JidPrepManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JidPrepManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public JidPrepManager(XMPPConnection connection) {
        super(connection);

        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    public String requestJidPrep(String jidToBePrepped)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DomainBareJid serviceAddress = serviceDiscoveryManager.findService(NAMESPACE, true);
        return requestJidPrep(serviceAddress, jidToBePrepped);
    }

    public String requestJidPrep(Jid jidPrepService, String jidToBePrepped)
                    throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        JidPrepIq jidPrepRequest = new JidPrepIq(jidToBePrepped);
        jidPrepRequest.setTo(jidPrepService);

        JidPrepIq jidPrepResponse;
        try {
            jidPrepResponse = connection().sendIqRequestAndWaitForResponse(jidPrepRequest);
        } catch (XMPPErrorException e) {
            StanzaError stanzaError = e.getStanzaError();
            if (stanzaError.getCondition() == StanzaError.Condition.jid_malformed) {
                // jid-malformed is, sadly, returned if the jid can not be normalized. This means we can not distinguish
                // if the error is returned because e.g. the IQ's 'to' address was malformed (c.f. RFC 6120 § 8.3.3.8)
                // or if the JID to prep was malformed. Assume the later is the case and return 'null'.
                return null;
            }

            throw e;
        }

        return jidPrepResponse.getJid();
    }

    public boolean isSupported(Jid jid)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return serviceDiscoveryManager.supportsFeature(jid, NAMESPACE);
    }
}
