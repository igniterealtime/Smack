/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.util.StringUtils;

import java.util.*;

/**
 * Allows creation and management of accounts on an XMPP server.
 *
 * @see XMPPConnection#getAccountManager()
 * @author Matt Tucker
 */
public class AccountManager {

    private XMPPConnection connection;
    private Registration info = null;

    /**
     * Creates a new AccountManager instance.
     *
     * @param connection a connection to a XMPP server.
     */
    public AccountManager(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns true if the server supports creating new accounts. Many servers require
     * that you not be currently authenticated when creating new accounts, so the safest
     * behavior is to only create new accounts before having logged in to a server.
     *
     * @return true if the server support creating new accounts.
     */
    public boolean supportsAccountCreation() {
        try {
            if (info == null) {
                getRegistrationInfo();
            }
            return info.getType() != IQ.Type.ERROR;
        }
        catch (XMPPException xe) {
            return false;
        }
    }

    /**
     * Returns an Iterator for the (String) names of the required account attributes.
     * All attributes must be set when creating new accounts. The standard
     * attributes are as follows: <ul>
     *      <li>name -- the user's name.
     *      <li>first -- the user's first name.
     *      <li>last -- the user's last name.
     *      <li>email -- the user's email address.
     *      <li>city -- the user's city.
     *      <li>state -- the user's state.
     *      <li>zip -- the user's ZIP code.
     *      <li>phone -- the user's phone number.
     *      <li>url -- the user's website.
     *      <li>date -- the date the registration took place.
     *      <li>misc -- other miscellaneous information to associate with the account.
     *      <li>text -- textual information to associate with the account.
     *      <li>remove -- empty flag to remove account.
     * </ul><p>
     *
     * Typically, servers require no attributes when creating new accounts, or just
     * the user's email address.
     *
     * @return the required account attributes.
     */
    public Iterator getAccountAttributes() {
        try {
            if (info == null) {
                getRegistrationInfo();
            }
            Map attributes = info.getAttributes();
            if (attributes != null) {
                return attributes.keySet().iterator();
            }
        }
        catch (XMPPException xe) { }
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Returns the value of a given account attribute or <tt>null</tt> if the account
     * attribute wasn't found.
     *
     * @param name the name of the account attribute to return its value.
     * @return the value of the account attribute or <tt>null</tt> if an account
     * attribute wasn't found for the requested name.
     */
    public String getAccountAttribute(String name) {
        try {
            if (info == null) {
                getRegistrationInfo();
            }
            return (String) info.getAttributes().get(name);
        }
        catch (XMPPException xe) { }
        return null;
    }

    /**
     * Returns the instructions for creating a new account, or <tt>null</tt> if there
     * are no instructions. If present, instructions should be displayed to the end-user
     * that will complete the registration process.
     *
     * @return the account creation instructions, or <tt>null</tt> if there are none.
     */
    public String getAccountInstructions() {
        try {
            if (info == null) {
                getRegistrationInfo();
            }
            return info.getInstructions();
        }
        catch (XMPPException xe) {
            return null;
        }
    }

    /**
     * Creates a new account using the specified username and password. The server may
     * require a number of extra account attributes such as an email address and phone
     * number. In that case, Smack will attempt to automatically set all required
     * attributes with blank values, which may or may not be accepted by the server.
     * Therefore, it's recommended to check the required account attributes and to let
     * the end-user populate them with real values instead.
     *
     * @param username the username.
     * @param password the password.
     * @throws XMPPException if an error occurs creating the account.
     */
    public void createAccount(String username, String password) throws XMPPException {
        if (!supportsAccountCreation()) {
            throw new XMPPException("Server does not support account creation.");
        }
        // Create a map for all the required attributes, but give them blank values.
        Map attributes = new HashMap();
        for (Iterator i=getAccountAttributes(); i.hasNext(); ) {
            String attributeName = (String)i.next();
            attributes.put(attributeName, "");
        }
        createAccount(username, password, attributes);
    }

    /**
     * Creates a new account using the specified username, password and account attributes.
     * The attributes Map must contain only String name/value pairs and must also have values
     * for all required attributes.
     *
     * @param username the username.
     * @param password the password.
     * @param attributes the account attributes.
     * @throws XMPPException if an error occurs creating the account.
     * @see #getAccountAttributes()
     */
    public void createAccount(String username, String password, Map attributes)
            throws XMPPException
    {
        if (!supportsAccountCreation()) {
            throw new XMPPException("Server does not support account creation.");
        }
        Registration reg = new Registration();
        reg.setType(IQ.Type.SET);
        reg.setTo(connection.getHost());
        reg.setUsername(username);
        reg.setPassword(password);
        reg.setAttributes(attributes);
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    /**
     * Changes the password of the currently logged-in account. This operation can only
     * be performed after a successful login operation has been completed. Not all servers
     * support changing passwords; an XMPPException will be thrown when that is the case.
     *
     * @throws IllegalStateException if not currently logged-in to the server.
     * @throws XMPPException if an error occurs when changing the password.
     */
    public void changePassword(String newPassword) throws XMPPException {
        Registration reg = new Registration();
        reg.setType(IQ.Type.SET);
        reg.setTo(connection.getHost());
        reg.setUsername(StringUtils.parseName(connection.getUser()));
        reg.setPassword(newPassword);
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    /**
     * Deletes the currently logged-in account from the server. This operation can only
     * be performed after a successful login operation has been completed. Not all servers
     * support deleting accounts; an XMPPException will be thrown when that is the case.
     *
     * @throws IllegalStateException if not currently logged-in to the server.
     * @throws XMPPException if an error occurs when deleting the account.
     */
    public void deleteAccount() throws XMPPException {
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must be logged in to delete a account.");
        }
        Registration reg = new Registration();
        reg.setType(IQ.Type.SET);
        reg.setTo(connection.getHost());
        Map attributes = new HashMap();
        // To delete an account, we add a single attribute, "remove", that is blank.
        attributes.put("remove", "");
        reg.setAttributes(attributes);
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    /**
     * Gets the account registration info from the server.
     *
     * @throws XMPPException if an error occurs.
     */
    private synchronized void getRegistrationInfo() throws XMPPException {
        Registration reg = new Registration();
        reg.setTo(connection.getHost());
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        else {
            info = (Registration)result;
        }
    }
}