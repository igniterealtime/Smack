package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.filter.*;

import java.util.*;

/**
 * Allows creation and management of accounts on an XMPP server.
 *
 * @see XMPPConnection#getAccountManager();
 * @author Matt Tucker
 */
public class AccountManager {

    private XMPPConnection connection;
    private Registration info = null;

    public AccountManager(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns true if the server supports creating new accounts. Many servers require that
     * you not be currently authenticated when creating new accounts, so the safest
     * behavior is to only create new accounts when
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

    public void createAccount(String username, String password) throws XMPPException {
        if (!supportsAccountCreation()) {
            throw new XMPPException("Server does not support account creation.");
        }
    }

    public void createAccount(String username, String password, Map attributes)
            throws XMPPException
    {

    }

    public void deleteAccount() throws XMPPException {
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must be logged in to delete a account.");
        }
        Registration reg = new Registration();
        reg.setType(IQ.Type.SET);
        Map attributes = new HashMap();
        // To delete an account, we add a single attribute, "remove", that is blank.
        attributes.put("remove", "");
        reg.setAttributes(attributes);
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(Registration.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(5000);
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    private synchronized void getRegistrationInfo() throws XMPPException {
        Registration reg = new Registration();
        PacketFilter filter = new AndFilter(new PacketIDFilter(reg.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ)collector.nextResult(5000);
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