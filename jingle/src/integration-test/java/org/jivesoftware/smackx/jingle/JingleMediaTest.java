package org.jivesoftware.smackx.jingle;

/**
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.TCPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.jmf.AudioChannel;
import org.jivesoftware.smackx.jingle.mediaimpl.jmf.JmfMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.jspeex.SpeexMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.multi.MultiMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.sshare.ScreenShareMediaManager;
import org.jivesoftware.smackx.jingle.nat.BridgedTransportManager;
import org.jivesoftware.smackx.jingle.nat.ICETransportManager;
import org.jivesoftware.smackx.jingle.nat.STUNTransportManager;
import org.jivesoftware.smackx.jingle.packet.JingleError;

import javax.media.MediaLocator;
import javax.media.format.AudioFormat;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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

        TCPConnection x0 = getConnection(0);
        TCPConnection x1 = getConnection(1);

        for (int i = 0; i < 1; i++)
            try {

                ICETransportManager icetm0 = new ICETransportManager(x0, "jivesoftware.com", 3478);
                ICETransportManager icetm1 = new ICETransportManager(x1, "jivesoftware.com", 3478);

                JingleMediaManager jingleMediaManager0 = new JmfMediaManager(icetm0);
                JingleMediaManager jingleMediaManager1 = new JmfMediaManager(icetm1);

                List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
                List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();

                jml0.add(jingleMediaManager0);
                jml1.add(jingleMediaManager1);

                final JingleManager jm0 = new JingleManager(x0, jml0);
                final JingleManager jm1 = new JingleManager(x1, jml1);

                jm0.addCreationListener(icetm0);
                jm1.addCreationListener(icetm1);

                JingleSessionRequestListener jingleSessionRequestListener = new JingleSessionRequestListener() {
                    public void sessionRequested(final JingleSessionRequest request) {
                        try {
                            JingleSession session = request.accept();
                            session.startIncoming();

                            //                            session.addStateListener(new JingleSessionStateListener() {
                            //                                public void beforeChange(JingleNegotiator.State old, JingleNegotiator.State newOne)
                            //                                        throws JingleNegotiator.JingleException {
                            //                                    if (newOne instanceof IncomingJingleSession.Active) {
                            //                                        throw new JingleNegotiator.JingleException();
                            //                                    }
                            //                                }
                            //
                            //                                public void afterChanged(JingleNegotiator.State old, JingleNegotiator.State newOne) {
                            //
                            //                                }
                            //                            });

                        } catch (XMPPException e) {
                            e.printStackTrace();
                        }

                    }
                };

                jm1.addJingleSessionRequestListener(jingleSessionRequestListener);

                JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

                js0.startOutgoing();

                Thread.sleep(20000);

                JingleSession incomingJingleSession = jm1.getSession(js0.getConnection().getUser());
                //JingleSession.removeAllStateListeners();

                Thread.sleep(15000);

                js0.terminate();

                jm1.removeJingleSessionRequestListener(jingleSessionRequestListener);

                Thread.sleep(60000);

            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void testCompleteMulti() {

        try {

            TCPConnection x0 = getConnection(0);
            TCPConnection x1 = getConnection(1);

            ICETransportManager icetm0 = new ICETransportManager(x0, "jivesoftware.com", 3478);
            ICETransportManager icetm1 = new ICETransportManager(x1, "jivesoftware.com", 3478);

            MultiMediaManager jingleMediaManager0 = new MultiMediaManager(icetm0);
            jingleMediaManager0.addMediaManager(new JmfMediaManager(icetm0));
            jingleMediaManager0.addMediaManager(new SpeexMediaManager(icetm0));
            jingleMediaManager0.setPreferredPayloadType(jingleMediaManager0.getPayloads().get(1));
            List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
            jml0.add(jingleMediaManager0);

            MultiMediaManager jingleMediaManager1 = new MultiMediaManager(icetm1);
            jingleMediaManager1.addMediaManager(new JmfMediaManager(icetm1));
            jingleMediaManager1.addMediaManager(new SpeexMediaManager(icetm1));
            jingleMediaManager1.setPreferredPayloadType(jingleMediaManager1.getPayloads().get(2));
            List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();
            jml1.add(jingleMediaManager1);

            final JingleManager jm0 = new JingleManager(x0, jml0);
            final JingleManager jm1 = new JingleManager(x1, jml1);

            jm0.addCreationListener(icetm0);
            jm1.addCreationListener(icetm1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {
                        JingleSession session = request.accept();
                        try {
                            Thread.sleep(12000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        session.startIncoming();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            for (int i = 0; i < 10; i++) {

                JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

                //                js0.addStateListener(new JingleSessionStateListener() {
                //
                //                    public void beforeChange(JingleNegotiator.State old, JingleNegotiator.State newOne)
                //                            throws JingleNegotiator.JingleException {
                //                    }
                //
                //                    public void afterChanged(JingleNegotiator.State old, JingleNegotiator.State newOne) {
                //                        if (newOne != null) {
                //                            if ((newOne instanceof OutgoingJingleSession.Active))
                //                                System.err.println("|||" + newOne.getClass().getCanonicalName() + "|||");
                //                        }
                //                    }
                //                });

                js0.startOutgoing();

                Thread.sleep(45000);
                js0.terminate();

                Thread.sleep(1500);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteSpeex() {

        try {

            //TCPConnection.DEBUG_ENABLED = true;

            TCPConnection x0 = getConnection(0);
            TCPConnection x1 = getConnection(1);

            JingleMediaManager jingleMediaManager0 = new SpeexMediaManager(new STUNTransportManager());
            JingleMediaManager jingleMediaManager1 = new SpeexMediaManager(new STUNTransportManager());

            List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
            List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();

            jml0.add(jingleMediaManager0);
            jml1.add(jingleMediaManager1);

            final JingleManager jm0 = new JingleManager(x0, jml0);
            final JingleManager jm1 = new JingleManager(x1, jml1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        JingleSession session = request.accept();

                        session.startIncoming();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.startOutgoing();

            Thread.sleep(150000);
            js0.terminate();

            Thread.sleep(6000);

            x0.disconnect();
            x1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testCompleteScreenShare() {

        try {

            TCPConnection x0 = getConnection(0);
            TCPConnection x1 = getConnection(1);

             ICETransportManager icetm0 = new ICETransportManager(x0, "stun.xten.net", 3478);
            ICETransportManager icetm1 = new ICETransportManager(x1, "stun.xten.net", 3478);

            JingleMediaManager jingleMediaManager0 = new ScreenShareMediaManager(icetm0);
            JingleMediaManager jingleMediaManager1 = new ScreenShareMediaManager(icetm1);

            List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
            List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();

            jml0.add(jingleMediaManager0);
            jml1.add(jingleMediaManager1);

            final JingleManager jm0 = new JingleManager(x0, jml0);
            final JingleManager jm1 = new JingleManager(x1, jml1);
            
            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        JingleSession session = request.accept();

                        session.startIncoming();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.startOutgoing();

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

                        TCPConnection x0 = getConnection(n);
                        TCPConnection x1 = getConnection(n + 1);
   
                        BridgedTransportManager btm0 = new BridgedTransportManager(x0);
                        BridgedTransportManager btm1 = new BridgedTransportManager(x1);


                        JingleMediaManager jingleMediaManager0 = new JmfMediaManager(btm0);
                        JingleMediaManager jingleMediaManager1 = new JmfMediaManager(btm1);

                        List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
                        List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();

                        jml0.add(jingleMediaManager0);
                        jml1.add(jingleMediaManager1);

                        final JingleManager jm0 = new JingleManager(x0, jml0);
                        final JingleManager jm1 = new JingleManager(x1, jml1);

                        jm0.addCreationListener(btm0);
                        jm1.addCreationListener(btm1);
                        
                        jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                            public void sessionRequested(final JingleSessionRequest request) {

                                try {
                                    JingleSession session = request.accept();

                                    session.startIncoming();
                                } catch (XMPPException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                        JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

                        js0.startOutgoing();

                        Thread.sleep(20000);

                        //js0.sendFormattedError(JingleError.UNSUPPORTED_TRANSPORTS);
                        js0.sendPacket(js0.createJingleError(null, JingleError.UNSUPPORTED_TRANSPORTS));
                        

                        Thread.sleep(20000);

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
            e.printStackTrace();
        }
    }

    public void testCompleteWithBridgeB() {
        try {

            //TCPConnection.DEBUG_ENABLED = true;

            TCPConnection x0 = getConnection(0);
            TCPConnection x1 = getConnection(1);

            BridgedTransportManager btm0 = new BridgedTransportManager(x0);
            BridgedTransportManager btm1 = new BridgedTransportManager(x1);


            JingleMediaManager jingleMediaManager0 = new JmfMediaManager(btm0);
            JingleMediaManager jingleMediaManager1 = new JmfMediaManager(btm1);

            List<JingleMediaManager> jml0 = new ArrayList<JingleMediaManager>();
            List<JingleMediaManager> jml1 = new ArrayList<JingleMediaManager>();

            jml0.add(jingleMediaManager0);
            jml1.add(jingleMediaManager1);

            final JingleManager jm0 = new JingleManager(x0, jml0);
            final JingleManager jm1 = new JingleManager(x1, jml1);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        JingleSession session = request.accept();

                        session.startIncoming();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            JingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.startOutgoing();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.startOutgoing();

            Thread.sleep(20000);

            js0.terminate();

            Thread.sleep(3000);

            x0.disconnect();
            x1.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testAudioChannelOpenClose() {
        for (int i = 0; i < 5; i++) {
            try {
                AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost()
                        .getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020, new AudioFormat(
                        AudioFormat.GSM_RTP), null);
                AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost()
                        .getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002, new AudioFormat(
                        AudioFormat.GSM_RTP), null);

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

    public void testAudioChannelStartStop() {

        try {
            AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost()
                    .getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7002, 7020,
                    new AudioFormat(AudioFormat.GSM_RTP), null);
            AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://"), InetAddress.getLocalHost()
                    .getHostAddress(), InetAddress.getLocalHost().getHostAddress(), 7020, 7002,
                    new AudioFormat(AudioFormat.GSM_RTP), null);

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

    protected int getMaxConnections() {
        return 2;
    }
}
