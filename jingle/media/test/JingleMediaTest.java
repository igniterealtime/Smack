import junit.framework.TestCase;
import org.jivesoftware.jingleaudio.jmf.AudioChannel;
import org.jivesoftware.jingleaudio.jmf.JmfMediaManager;
import org.jivesoftware.jingleaudio.jspeex.SpeexMediaManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.nat.BridgedTransportManager;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.STUNTransportManager;

import javax.media.MediaLocator;
import javax.media.format.AudioFormat;
import java.net.InetAddress;

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
public class JingleMediaTest extends TestCase {

    public void testCompleteJmf() {

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = new XMPPConnection("thiago");
            XMPPConnection x1 = new XMPPConnection("thiago");

            x0.connect();
            x0.login("barata7", "barata7");
            x1.connect();
            x1.login("barata6", "barata6");

            final JingleManager jm0 = new JingleManager(
                    x0, new ICETransportManager());
            final JingleManager jm1 = new JingleManager(
                    x1, new ICETransportManager());

            JingleMediaManager jingleMediaManager0 = new JmfMediaManager();
            JingleMediaManager jingleMediaManager1 = new JmfMediaManager();

            jm0.setMediaManager(jingleMediaManager0);
            jm1.setMediaManager(jingleMediaManager1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {
                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());
                        session.start(request);
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("barata6@thiago/Smack");

            js0.start();

            Thread.sleep(50000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteSpeex() {

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = new XMPPConnection("thiago");
            XMPPConnection x1 = new XMPPConnection("thiago");

            x0.connect();
            x0.login("barata7", "barata7");
            x1.connect();
            x1.login("barata6", "barata6");

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
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("barata6@thiago/Smack");

            js0.start();

            Thread.sleep(150000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteWithBridge() {

        for (int i = 0; i < 1; i += 2) {
            final int n = i;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {

                        XMPPConnection x0 = new XMPPConnection("thiago");
                        XMPPConnection x1 = new XMPPConnection("thiago");

                        x0.connect();
                        x0.login("user" + String.valueOf(n), "user" + String.valueOf(n));
                        x1.connect();
                        x1.login("user" + String.valueOf(n + 1), "user" + String.valueOf(n + 1));

                        BridgedTransportManager btm0 = new BridgedTransportManager(x0);
                        BridgedTransportManager btm1 = new BridgedTransportManager(x1);

                        final JingleManager jm0 = new JingleManager(x0, btm0);
                        final JingleManager jm1 = new JingleManager(x1, btm1);

                        jm0.addCreationListener(btm0);
                        jm1.addCreationListener(btm1);

                        JingleMediaManager jingleMediaManager = new SpeexMediaManager();
                        JingleMediaManager jingleMediaManager2 = new SpeexMediaManager();

                        jm0.setMediaManager(jingleMediaManager);
                        jm1.setMediaManager(jingleMediaManager2);

                        jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                            public void sessionRequested(final JingleSessionRequest request) {

                                try {
                                    IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());

                                    session.start(request);
                                } catch (XMPPException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                        OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("user" + String.valueOf(n + 1) + "@thiago/Smack");

                        js0.start();

                        Thread.sleep(55000);

                        js0.terminate();

                        Thread.sleep(3000);

                        x0.disconnect();
                        x1.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();
        }

        try {
            Thread.sleep(250000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void testCompleteWithBridgeB() {
        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = new XMPPConnection("thiago");
            XMPPConnection x1 = new XMPPConnection("thiago");

            x0.connect();
            x0.login("barata5", "barata5");
            x1.connect();
            x1.login("barata4", "barata4");

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
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("barata4@thiago/Smack");

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            js0 = jm0.createOutgoingJingleSession("barata4@thiago/Smack");

            js0.start();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            x0.disconnect();
            x1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testAudioChannelOpenClose
            () {
        for (int i = 0; i < 5; i++) {
            try {
                AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP));
                AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP));

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void testAudioChannelStartStop
            () {

        try {
            AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP));
            AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost().getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP));

            for (int i = 0; i < 5; i++) {

                audioChannel0.start();
                audioChannel1.start();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                audioChannel0.stop();
                audioChannel1.stop();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}