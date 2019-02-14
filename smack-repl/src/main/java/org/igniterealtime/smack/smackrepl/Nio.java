/**
 *
 * Copyright 2018 Florian Schmaus
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
