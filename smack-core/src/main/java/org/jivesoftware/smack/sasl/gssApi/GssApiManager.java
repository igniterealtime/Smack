package org.jivesoftware.smack.sasl.gssApi;

import org.ietf.jgss.*;
import org.jivesoftware.smack.XMPPConnection;

import java.security.Provider;

public class GssApiManager {

    GSSManager gssManager;
    GSSName userName = null;
    GSSName serverName = null;
    GSSCredential userCredential = null;
    GSSContext clientContext = null;

    Oid objectIdentifiers;
    byte[] clientToken;

    private static GssApiManager gssApiManager = null;

    public static synchronized GssApiManager getInstanceFor(XMPPConnection connection) {
        if (gssApiManager == null) {
            return new GssApiManager(connection);
        }
        return gssApiManager;
    }

    private GssApiManager(XMPPConnection connection){
        gssManager = GSSManager.getInstance();

        try {
            userName = gssManager.createName(connection.getUser().asEntityBareJid().asEntityBareJidString(),GSSName.NT_USER_NAME);
            serverName = gssManager.createName(connection.getHost(),objectIdentifiers);
            userCredential = gssManager.createCredential(userName,GSSCredential.DEFAULT_LIFETIME,objectIdentifiers,GSSCredential.INITIATE_ONLY);
            clientContext = gssManager.createContext(serverName,objectIdentifiers,userCredential,GSSContext.DEFAULT_LIFETIME);
            clientContext.requestMutualAuth(true);
            clientContext.requestConf(true);
            clientContext.requestInteg(true);
        } catch (GSSException e) {
            e.printStackTrace();
        }
    }

    public byte[] authenticate() throws GSSException {
        clientToken = clientContext.initSecContext(new byte[0], 0, 0);

        // Send client token over the network
        return clientToken;
    }

    public byte[] acceptServerToken(byte[] serverToken) {
        try {
            return clientContext.initSecContext(serverToken,0,serverToken.length);
        } catch (GSSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isContextEstablished() {
        return clientContext.isEstablished();
    }

    public byte[] sendData(byte[] messageBytes) throws GSSException {
        MessageProp clientProp = new MessageProp(0,true);
        clientToken = clientContext.wrap(messageBytes,0,messageBytes.length,clientProp);

        // Send client token over the network
        return clientToken;
    }

}
