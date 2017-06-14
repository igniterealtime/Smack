/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.igniterealtime.smack.smackrepl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;

import org.jivesoftware.smackx.iqregister.AccountManager;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;

public class XmppTools {

    public static void main(String[] args) throws SmackException, IOException, XMPPException, InterruptedException,
            KeyManagementException, NoSuchAlgorithmException {
        boolean one = createAccount("xmpp.foobar.io", "smack1", "smack1");
        boolean two = createAccount("xmpp.foobar.io", "smack2", "smack2");
        // CHECKSTYLE:OFF
        System.out.println("Account created: " + one + ' ' + two);
        // CHECKSTYLE:ON
    }

    public static boolean supportsIbr(String xmppDomain) throws SmackException, IOException, XMPPException,
            InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        DomainBareJid xmppDomainJid = JidCreate.domainBareFrom(xmppDomain);
        return supportsIbr(xmppDomainJid);
    }

    public static boolean supportsIbr(DomainBareJid xmppDomain) throws SmackException, IOException, XMPPException,
            InterruptedException, KeyManagementException, NoSuchAlgorithmException {
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(xmppDomain);
        TLSUtils.acceptAllCertificates(configBuilder);
        XMPPTCPConnectionConfiguration config = configBuilder.build();
        XMPPTCPConnection connection = new XMPPTCPConnection(config);
        connection.connect();
        try {
            return supportsIbr(connection);
        } finally {
            connection.disconnect();
        }
    }

    public static boolean supportsIbr(XMPPConnection connection)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AccountManager accountManager = AccountManager.getInstance(connection);
        return accountManager.supportsAccountCreation();
    }

    public static boolean createAccount(String xmppDomain, String username, String password)
            throws KeyManagementException, NoSuchAlgorithmException, SmackException, IOException, XMPPException,
            InterruptedException {
        DomainBareJid xmppDomainJid = JidCreate.domainBareFrom(xmppDomain);
        Localpart localpart = Localpart.from(username);
        return createAccount(xmppDomainJid, localpart, password);
    }

    public static boolean createAccount(DomainBareJid xmppDomain, Localpart username, String password)
            throws KeyManagementException, NoSuchAlgorithmException, SmackException, IOException, XMPPException,
            InterruptedException {
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(xmppDomain);
        TLSUtils.acceptAllCertificates(configBuilder);
        XMPPTCPConnectionConfiguration config = configBuilder.build();
        XMPPTCPConnection connection = new XMPPTCPConnection(config);
        connection.connect();
        try {
            if (!supportsIbr(connection))
                return false;

            AccountManager accountManager = AccountManager.getInstance(connection);
            accountManager.createAccount(username, password);
            return true;
        } finally {
            connection.disconnect();
        }
    }
}
