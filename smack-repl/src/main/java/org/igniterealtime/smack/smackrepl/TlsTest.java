/**
 *
 * Copyright 2016 Florian Schmaus
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.smackrepl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;

import eu.geekplace.javapinning.java7.Java7Pinning;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class TlsTest {

//    private static final Logger LOGGER = Logger.getLogger(TlsTest.class.getName());
    public static boolean DEBUG = false;

    public static void main(String[] args) throws XmppStringprepException, KeyManagementException, NoSuchAlgorithmException {
        DEBUG = true;
        tlsTest(args[0], args[1], args[2], args[3], args[4], args[5], true);
    }

    public static void tlsTest(String runsString, CharSequence jidCs, String password, String host, String portString,
                    String tlsPin, boolean shouldThrow) throws XmppStringprepException, KeyManagementException, NoSuchAlgorithmException {
        int runs = Integer.parseInt(runsString);
        int port = Integer.parseInt(portString);
        tlsTest(runs, jidCs, password, host, port, tlsPin, shouldThrow);
    }

    public static void tlsTest(int runs, CharSequence jidCs, String password, String host, int port,
                    String tlsPin, boolean shouldThrow) throws XmppStringprepException, KeyManagementException, NoSuchAlgorithmException {
        EntityBareJid jid = JidCreate.entityBareFrom(jidCs);
        for (int i = 0; i < runs; i++) {
            boolean res = tlsTest(jid, password, host, port, tlsPin, shouldThrow);
            if (!res) {
                throw new IllegalStateException();
            }
        }
    }

    public static boolean tlsTest(CharSequence jidCs, String password, String host, int port,
                    String tlsPin, boolean shouldThrow) throws XmppStringprepException, KeyManagementException, NoSuchAlgorithmException {
        EntityBareJid jid = JidCreate.entityBareFrom(jidCs);
        return tlsTest(jid, password, host, port, tlsPin, shouldThrow);
    }

    public static boolean tlsTest(EntityBareJid jid, String password, String host, int port,
                    String tlsPin, boolean shouldThrow) throws KeyManagementException, NoSuchAlgorithmException {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        // @formatter:off
        builder.setUsernameAndPassword(jid.getLocalpart(), password)
               .setXmppDomain(JidCreate.domainBareFrom(jid.getDomain()))
               .setHost(host)
               .setPort(port)
               .setSecurityMode(SecurityMode.required);
        // @formatter:on
        if (DEBUG) {
            builder.enableDefaultDebugger();
        }

        if (StringUtils.isNotEmpty(tlsPin)) {
            SSLContext sslContext = Java7Pinning.forPin(tlsPin);
            builder.setSslContextFactory(() -> sslContext);
        }


        XMPPTCPConnection connection = new XMPPTCPConnection(builder.build());

        connection.setReplyTimeout(20000);

        try {
            connection.connect().login();
            if (shouldThrow) {
                // Test not success, should have thrown on login().
                return false;
            }
        }
        catch (SecurityRequiredByClientException e) {
            if (!shouldThrow) {
                return false;
            }
        }
        catch (XMPPException | SmackException | IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
        finally {
            connection.disconnect();
        }

        return true;
    }
}
