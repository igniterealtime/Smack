/**
 *
 * Copyright 2018 Florian Schmaus
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
import java.util.Date;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.compression.XMPPInputOutputStream.FlushMethod;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.tcp.XmppNioTcpConnection;

import org.jxmpp.util.XmppDateTime;

public class Nio {

    private static final Logger LOGGER = Logger.getLogger(Nio.class.getName());

    public static void main(String[] args) throws SmackException, IOException, XMPPException, InterruptedException {
        doNio(args[0], args[1], args[2]);
    }

    public static void doNio(String username, String password, String service)
            throws SmackException, IOException, XMPPException, InterruptedException {
        boolean useTls = true;
        boolean useCompression = true;
        boolean useFullFlush = true;
        boolean javaNetDebug = false;
        boolean smackDebug = false;

        if (useFullFlush) {
            XMPPInputOutputStream.setFlushMethod(FlushMethod.FULL_FLUSH);
        }

        if (javaNetDebug) {
            System.setProperty("javax.net.debug", "all");
        }

        final SecurityMode securityMode;
        if (useTls) {
            securityMode = SecurityMode.required;
        } else {
            securityMode = SecurityMode.disabled;
        }

        final SmackDebuggerFactory smackDebuggerFactory;
        if (smackDebug) {
            smackDebuggerFactory = ConsoleDebugger.Factory.INSTANCE;
        } else {
            smackDebuggerFactory = null;
        }

        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(username, password)
                .setXmppDomain(service)
                .setDebuggerFactory(smackDebuggerFactory)
                .setCompressionEnabled(useCompression)
                .setSecurityMode(securityMode)
                .build();

        XmppNioTcpConnection connection = new XmppNioTcpConnection(configuration);

        connection.setReplyTimeout(5 * 60 * 1000);

        connection.addConnectionStateMachineListener((event, c) -> {
            LOGGER.info("Connection event: " + event);
        });

        connection.connect();

        connection.login();

        Message message = new Message("flo@geekplace.eu",
                "It is alive! " + XmppDateTime.formatXEP0082Date(new Date()));
        connection.sendStanza(message);

        Thread.sleep(1000);

        connection.disconnect();

        XmppNioTcpConnection.Stats connectionStats = connection.getStats();

        // CHECKSTYLE:OFF
        System.out.println("NIO successfully finished, yeah!\n" + connectionStats);
        // CHECKSTYLE:ON
    }

}
