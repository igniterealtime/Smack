/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.nat;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;

/**
 * The FixedResolver is a resolver where
 * the external address and port are previously known when the object is
 * initialized.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public class FixedResolver extends TransportResolver {

    TransportCandidate fixedCandidate;

    /**
     * Constructor.
     */
    public FixedResolver(String ip, int port) {
        super();
        setFixedCandidate(ip, port);
    }

    /**
     * Create a basic resolver, where we provide the IP and port.
     *
     * @param ip   an IP address
     * @param port a port
     */
    public void setFixedCandidate(String ip, int port) {
        fixedCandidate = new TransportCandidate.Fixed(ip, port);
    }

    /**
     * Resolve the IP address.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    @Override
    public synchronized void resolve(JingleSession session) throws XMPPException, NotConnectedException, InterruptedException {
        if (!isResolving()) {
            setResolveInit();

            clearCandidates();

            if (fixedCandidate.getLocalIp() == null)
                fixedCandidate.setLocalIp(fixedCandidate.getIp());

            if (fixedCandidate != null) {
                addCandidate(fixedCandidate);
            }

            setResolveEnd();
        }
    }

    /**
     * Initialize the resolver.
     *
     * @throws XMPPException
     */
    @Override
    public void initialize() throws XMPPException {
        setInitialized();
    }

    @Override
    public void cancel() throws XMPPException {
        // Nothing to do here
    }
}
