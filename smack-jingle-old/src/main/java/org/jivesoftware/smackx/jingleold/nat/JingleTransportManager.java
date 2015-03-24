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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;

/**
 * Transport manager for Jingle.
 *
 * This class makes easier the use of transport resolvers by presenting a simple
 * interface for algorithm selection. The transport manager also keeps the match
 * between the resolution method and the &lt;transport&gt; element present in
 * Jingle packets.
 *
 * As Jingle have many transport methods (official and unofficial methods),
 * this abstract class helps us to extends the transport support of the API.
 *
 * This class must be used with a JingleManager instance in the following way:
 *
 * JingleManager jingleManager = new JingleManager(xmppConnection, new BasicTransportManager());
 *
 * @author Thiago Camargo
 */
public abstract class JingleTransportManager {
    // This class implements the context of a Strategy pattern...

    /**
     * Deafult contructor.
     */
    public JingleTransportManager() {

    }

    /**
     * Get a new Transport Resolver to be used in a Jingle Session
     *
     * @return the TransportResolver to be used
     * @throws InterruptedException 
     */
    public TransportResolver getResolver(JingleSession session) throws XMPPException, SmackException, InterruptedException {
        TransportResolver resolver = createResolver(session);
        if (resolver == null) {
            resolver = new BasicResolver();
        }
        resolver.initializeAndWait();

        return resolver;
    }

    /**
     * Create a Transport Resolver instance according to the implementation.
     *
     * @return the TransportResolver
     * @throws InterruptedException 
     */
    protected abstract TransportResolver createResolver(JingleSession session) throws SmackException, InterruptedException;

}
