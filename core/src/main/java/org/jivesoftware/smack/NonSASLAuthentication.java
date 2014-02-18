/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.Callback;

/**
 * Implementation of JEP-0078: Non-SASL Authentication. Follow the following
 * <a href=http://www.jabber.org/jeps/jep-0078.html>link</a> to obtain more
 * information about the JEP.
 *
 * @author Gaston Dombiak
 */
class NonSASLAuthentication implements UserAuthentication {

    private Connection connection;

    public NonSASLAuthentication(Connection connection) {
        super();
        this.connection = connection;
    }

    public String authenticate(String username, String resource, CallbackHandler cbh) throws XMPPException {
        //Use the callback handler to determine the password, and continue on.
        PasswordCallback pcb = new PasswordCallback("Password: ",false);
        try {
            cbh.handle(new Callback[]{pcb});
            return authenticate(username, String.valueOf(pcb.getPassword()),resource);
        } catch (Exception e) {
            throw new XMPPException("Unable to determine password.",e);
        }   
    }

    public String authenticate(String username, String password, String resource) throws
            XMPPException {
        // If we send an authentication packet in "get" mode with just the username,
        // the server will return the list of authentication protocols it supports.
        Authentication discoveryAuth = new Authentication();
        discoveryAuth.setType(IQ.Type.GET);
        discoveryAuth.setUsername(username);

        // Otherwise, no error so continue processing.
        Authentication authTypes = (Authentication) connection.createPacketCollectorAndSend(
                        discoveryAuth).nextResultOrThrow();

        // Now, create the authentication packet we'll send to the server.
        Authentication auth = new Authentication();
        auth.setUsername(username);

        // Figure out if we should use digest or plain text authentication.
        if (authTypes.getDigest() != null) {
            auth.setDigest(connection.getConnectionID(), password);
        }
        else if (authTypes.getPassword() != null) {
            auth.setPassword(password);
        }
        else {
            throw new XMPPException("Server does not support compatible authentication mechanism.");
        }

        auth.setResource(resource);

        Packet response = connection.createPacketCollectorAndSend(auth).nextResultOrThrow();

        return response.getTo();
    }

    public String authenticateAnonymously() throws XMPPException {
        // Create the authentication packet we'll send to the server.
        Authentication auth = new Authentication();

        Packet response = connection.createPacketCollectorAndSend(auth).nextResultOrThrow();

        if (response.getTo() != null) {
            return response.getTo();
        }
        else {
            return connection.getServiceName() + "/" + ((Authentication) response).getResource();
        }
    }
}
