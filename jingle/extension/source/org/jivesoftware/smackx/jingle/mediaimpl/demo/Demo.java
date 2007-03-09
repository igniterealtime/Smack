package org.jivesoftware.smackx.jingle.mediaimpl.jmf;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.nat.BridgedTransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.RTPBridge;
import org.jivesoftware.smackx.jingle.nat.STUNTransportManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * $RCSfile$
 * $Revision: $
 * $Date: 28/12/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class Demo extends JFrame {

    private JingleTransportManager transportManager = null;
    private XMPPConnection xmppConnection = null;

    private String server = null;
    private String user = null;
    private String pass = null;

    private JingleManager jm = null;
    private IncomingJingleSession incoming = null;
    private OutgoingJingleSession outgoing = null;

    private JTextField jid = new JTextField(30);

    public Demo(String server, String user, String pass) {

        this.server = server;
        this.user = user;
        this.pass = pass;

        xmppConnection = new XMPPConnection(server);
        try {
            xmppConnection.connect();
            xmppConnection.login(user, pass);
            initialize();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        if (RTPBridge.serviceAvailable(xmppConnection))
            transportManager = new BridgedTransportManager(xmppConnection);
        else
            transportManager = new STUNTransportManager();

        jm = new JingleManager(xmppConnection, transportManager, new JmfMediaManager());

        if (transportManager instanceof BridgedTransportManager)
            jm.addCreationListener((BridgedTransportManager) transportManager);

        jm.addJingleSessionRequestListener(new JingleSessionRequestListener() {
            public void sessionRequested(JingleSessionRequest request) {

                if (incoming != null)
                    return;

                try {
                    // Accept the call
                    incoming = request.accept();

                    // Start the call
                    incoming.start();
                }
                catch (XMPPException e) {
                    e.printStackTrace();
                }

            }
        });
        createGUI();
    }

    public void createGUI() {

        JPanel jPanel = new JPanel();

        jPanel.add(jid);

        jPanel.add(new JButton(new AbstractAction("Call") {
            public void actionPerformed(ActionEvent e) {
                if (outgoing != null) return;
                try {
                    outgoing = jm.createOutgoingJingleSession(jid.getText());
                } catch (XMPPException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        jPanel.add(new JButton(new AbstractAction("Hangup") {
            public void actionPerformed(ActionEvent e) {
                if (outgoing != null)
                    try {
                        outgoing.terminate();
                    } catch (XMPPException e1) {
                        e1.printStackTrace();
                    } finally {
                        outgoing = null;
                    }
                if (incoming != null)
                    try {
                        incoming.terminate();
                    } catch (XMPPException e1) {
                        e1.printStackTrace();
                    } finally {
                        incoming = null;
                    }
            }
        }));

        this.add(jPanel);

    }

    public static void main(String args[]) {

        Demo demo = null;

        if (args.length > 2) {
            demo = new Demo(args[0], args[1], args[2]);
            demo.pack();
            demo.setVisible(true);
            demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

    }

}
