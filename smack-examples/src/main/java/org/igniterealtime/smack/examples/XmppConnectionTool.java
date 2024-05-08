/**
 *
 * Copyright 2024 Florian Schmaus.
 *
 * This file is part of smack-examples.
 *
 * smack-examples is free software; you can redistribute it and/or modify
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
package org.igniterealtime.smack.examples;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.pep.PepManager;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.jxmpp.stringprep.XmppStringprepException;

public class XmppConnectionTool {

    public final ModularXmppClientToServerConnection connection;

    public XmppConnectionTool(ModularXmppClientToServerConnection connection) {
        this.connection = connection;
    }

    public boolean purgeOmemoInformation()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PepManager pepManager = PepManager.getInstanceFor(connection);
        PubSubManager pepPubSubManager = pepManager.getPepPubSubManager();

        // TODO: Also delete "bundles" nodes.
        return pepPubSubManager.deleteNode(OmemoConstants.PEP_NODE_DEVICE_LIST);
    }

    public static XmppConnectionTool of(String jid, String password, boolean debug)
            throws XMPPException, SmackException, IOException, InterruptedException {
        ModularXmppClientToServerConnection connection = createConnectionFor(jid, password, debug);
        connection.connect().login();
        return new XmppConnectionTool(connection);
    }

    public static ModularXmppClientToServerConnection createConnectionFor(String jid, String password, boolean debug)
            throws XmppStringprepException {

        final SmackDebuggerFactory smackDebuggerFactory;
        if (debug) {
            smackDebuggerFactory = ConsoleDebugger.Factory.INSTANCE;
        } else {
            smackDebuggerFactory = null;
        }

        ModularXmppClientToServerConnectionConfiguration.Builder configurationBuilder = ModularXmppClientToServerConnectionConfiguration
                .builder().setXmppAddressAndPassword(jid, password).setDebuggerFactory(smackDebuggerFactory);

        ModularXmppClientToServerConnectionConfiguration configuration = configurationBuilder.build();

        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(configuration);
        return connection;
    }
}
