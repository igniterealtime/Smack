/**
 * $RCSfile$
 * $Revision: $
 * $Date: 09/11/2006
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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.mediaimpl.jmf.JmfMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.jmf.AudioChannel;
import org.jivesoftware.smackx.jingle.mediaimpl.jspeex.SpeexMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.multi.MultiMediaManager;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.nat.BridgedTransportManager;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.STUNTransportManager;

import javax.media.MediaLocator;
import javax.media.format.AudioFormat;
import java.net.InetAddress;

/**
 * Test the Jingle Media using the high level API
 * </p>
 *
 * @author Thiago Camargo
 */
public class JingleMediaTest extends SmackTestCase {

    public JingleMediaTest(final String name) {
        super(name);
    }

    public void testCompleteJmf() {

        XMPPConnection x0 = getConnection(0);
        XMPPConnection x1 = getConnection(1);

        for (int i = 0; i < 3; i++)
            try {

                ICETransportManager icetm0 = new ICETransportManager(x0, "jivesoftware.com", 3478);
                ICETransportManager icetm1 = new ICETransportManager(x1, "jivesoftware.com", 3478);

                final JingleManager jm0 = new JingleManager(
                        x0, icetm0);
                final JingleManager jm1 = new JingleManager(
                        x1, icetm1);

                jm0.addCreationListener(icetm0);
                jm1.addCreationListener(icetm1);

                JingleMediaManager jingleMediaManager0 = new JmfMediaManager();
                JingleMediaManager jingleMediaManager1 = new JmfMediaManager();

                jm0.setMediaManager(jingleMediaManager0);
                jm1.setMediaManager(jingleMediaManager1);

                JingleSessionRequestListener jingleSessionRequestListener = new JingleSessionRequestListener() {
                    public void sessionRequested(final JingleSessionRequest request) {

                        try {
                            IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());
                            session.start(request);
                        }
                        catch (XMPPException e) {
                            e.printStackTrace();
                        }

                    }
                };

                jm1.addJingleSessionRequestListener(jingleSessionRequestListener);

                OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

                js0.start();

                Thread.sleep(50000);
                js0.terminate();

                jm1.removeJingleSessionRequestListener(jingleSessionRequestListener);

                Thread.sleep(6000);

            }
            catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void testCompleteMulti() {

        try {

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);


            ICETransportManager icetm0 = new ICETransportManager(x0, "jivesoftware.com", 3478);
            ICETransportManager icetm1 = new ICETransportManager(x1, "jivesoftware.com", 3478);

            final JingleManager jm0 = new JingleManager(
                    x0, icetm0);
            final JingleManager jm1 = new JingleManager(
                    x1, icetm1);

            jm0.addCreationListener(icetm0);
            jm1.addCreationListener(icetm1);

/*
          final JingleManager jm0 = new JingleManager(
                  x0, new BasicTransportManager());
          final JingleManager jm1 = new JingleManager(
                  x1, new BasicTransportManager());
*/

            MultiMediaManager jingleMediaManager0 = new MultiMediaManager();
            jingleMediaManager0.addMediaManager(new JmfMediaManager());
            jingleMediaManager0.addMediaManager(new SpeexMediaManager());
            MultiMediaManager jingleMediaManager1 = new MultiMediaManager();
            jingleMediaManager1.addMediaManager(new JmfMediaManager());
            jingleMediaManager1.addMediaManager(new SpeexMediaManager());
            jingleMediaManager1.setPreferredPayloadType(jingleMediaManager1.getPayloads().get(2));

            jm0.setMediaManager(jingleMediaManager0);
            jm1.setMediaManager(jingleMediaManager1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {
                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());
                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.start();

            Thread.sleep(60000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteSpeex() {

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(
                    x0, new STUNTransportManager());
            final JingleManager jm1 = new JingleManager(
                    x1, new STUNTransportManager());

            JingleMediaManager jingleMediaManager0 = new SpeexMediaManager();
            JingleMediaManager jingleMediaManager1 = new SpeexMediaManager();

            jm0.setMediaManager(jingleMediaManager0);
            jm1.setMediaManager(jingleMediaManager1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());

                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.start();

            Thread.sleep(150000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteWithBridge() {

        for (int i = 0; i < 1; i += 2) {
            final int n = i;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {

                        XMPPConnection x0 = getConnection(n);
                        XMPPConnection x1 = getConnection(n + 1);

                        BridgedTransportManager btm0 = new BridgedTransportManager(x0);
                        BridgedTransportManager btm1 = new BridgedTransportManager(x1);

                        final JingleManager jm0 = new JingleManager(x0, btm0);
                        final JingleManager jm1 = new JingleManager(x1, btm1);

                        jm0.addCreationListener(btm0);
                        jm1.addCreationListener(btm1);

                        JingleMediaManager jingleMediaManager = new JmfMediaManager();
                        JingleMediaManager jingleMediaManager2 = new JmfMediaManager();

                        jm0.setMediaManager(jingleMediaManager);
                        jm1.setMediaManager(jingleMediaManager2);

                        jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                            public void sessionRequested(final JingleSessionRequest request) {

                                try {
                                    IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());

                                    session.start(request);
                                }
                                catch (XMPPException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                        OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

                        js0.start();

                        Thread.sleep(55000);

                        js0.terminate();

                        Thread.sleep(3000);

                        x0.disconnect();
                        x1.disconnect();

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
        }

        try {
            Thread.sleep(250000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCompleteWithBridgeB() {
        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            BridgedTransportManager btm0 = new BridgedTransportManager(x0);
            BridgedTransportManager btm1 = new BridgedTransportManager(x1);

            final JingleManager jm0 = new JingleManager(x0, btm0);
            final JingleManager jm1 = new JingleManager(x1, btm1);

            jm0.addCreationListener(btm0);
            jm1.addCreationListener(btm1);

            JingleMediaManager jingleMediaManager = new JmfMediaManager();

            jm0.setMediaManager(jingleMediaManager);
            jm1.setMediaManager(jingleMediaManager);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());

                        session.start(request);
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            x0.disconnect();
            x1.disconnect();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testAudioChannelOpenClose() {
        for (int i = 0; i < 5; i++) {
            try {
                AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP));
                AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP));

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void testAudioChannelStartStop() {

        try {
            AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP));
            AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP));

            for (int i = 0; i < 5; i++) {

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}