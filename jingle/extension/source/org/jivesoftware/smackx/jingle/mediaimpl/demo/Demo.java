/**
 * $RCSfile: Demo.java,v $
 * $Revision: 1.3 $
 * $Date: 28/12/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.mediaimpl.demo;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.jspeex.SpeexMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.sshare.ScreenShareMediaManager;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Jingle Demo Application. It register in a XMPP Server and let users place calls using a full JID and auto-receive calls.
 * Parameters: Server User Pass.
 */
public class Demo extends JFrame {

	private static final long serialVersionUID = -6584021277434403855L;
	private JingleTransportManager transportManager = null;
    private Connection xmppConnection = null;

    private String server = null;
    private String user = null;
    private String pass = null;

    private JingleManager jm = null;
    private JingleSession incoming = null;
    private JingleSession outgoing = null;

    private JTextField jid;

    public Demo(String server, String user, String pass) {

        this.server = server;
        this.user = user;
        this.pass = pass;
        
        if (user.equals("jeffw")) {
            jid = new JTextField("eowyn" + "@" + server + "/Smack");
        } else {
            jid = new JTextField("jeffw" + "@" + server + "/Smack");
        }

        xmppConnection = new XMPPConnection(server);
        try {
            xmppConnection.connect();
            xmppConnection.login(user, pass);
            initialize();
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        ICETransportManager icetm0 = new ICETransportManager(xmppConnection, "10.47.47.53", 3478);
        List<JingleMediaManager> mediaManagers = new ArrayList<JingleMediaManager>();
        //mediaManagers.add(new JmfMediaManager(icetm0));
        mediaManagers.add(new SpeexMediaManager(icetm0));
        mediaManagers.add(new ScreenShareMediaManager(icetm0));
        jm = new JingleManager(xmppConnection, mediaManagers);
        jm.addCreationListener(icetm0);

        jm.addJingleSessionRequestListener(new JingleSessionRequestListener() {
            public void sessionRequested(JingleSessionRequest request) {

//                if (incoming != null)
//                    return;

                try {
                    // Accept the call
                    incoming = request.accept();

                    // Start the call
                    incoming.startIncoming();
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
			private static final long serialVersionUID = 4308448034795312815L;

			public void actionPerformed(ActionEvent e) {
                if (outgoing != null) return;
                try {
                    outgoing = jm.createOutgoingJingleSession(jid.getText());
                    outgoing.startOutgoing();
                }
                catch (XMPPException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        jPanel.add(new JButton(new AbstractAction("Hangup") {
			private static final long serialVersionUID = -4508007389146723587L;

			public void actionPerformed(ActionEvent e) {
                if (outgoing != null)
                    try {
                        outgoing.terminate();
                    }
                    catch (XMPPException e1) {
                        e1.printStackTrace();
                    }
                    finally {
                        outgoing = null;
                    }
                if (incoming != null)
                    try {
                        incoming.terminate();
                    }
                    catch (XMPPException e1) {
                        e1.printStackTrace();
                    }
                    finally {
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
