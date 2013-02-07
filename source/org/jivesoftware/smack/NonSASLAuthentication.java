/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;

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

        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(discoveryAuth.getPacketID()));
        // Send the packet
        connection.sendPacket(discoveryAuth);
        // Wait up to a certain number of seconds for a response from the server.
        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        // Otherwise, no error so continue processing.
        Authentication authTypes = (Authentication) response;
        collector.cancel();

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

        collector = connection.createPacketCollector(new PacketIDFilter(auth.getPacketID()));
        // Send the packet.
        connection.sendPacket(auth);
        // Wait up to a certain number of seconds for a response from the server.
        response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (response == null) {
            throw new XMPPException("Authentication failed.");
        }
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        // We're done with the collector, so explicitly cancel it.
        collector.cancel();

        return response.getTo();
    }

    public String authenticateAnonymously() throws XMPPException {
        // Create the authentication packet we'll send to the server.
        Authentication auth = new Authentication();

        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(auth.getPacketID()));
        // Send the packet.
        connection.sendPacket(auth);
        // Wait up to a certain number of seconds for a response from the server.
        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (response == null) {
            throw new XMPPException("Anonymous login failed.");
        }
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        // We're done with the collector, so explicitly cancel it.
        collector.cancel();

        if (response.getTo() != null) {
            return response.getTo();
        }
        else {
            return connection.getServiceName() + "/" + ((Authentication) response).getResource();
        }
    }
}
