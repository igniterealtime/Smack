/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
/**
 * $RCSfile: JingleManagerTest.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/07/18 18:29:21 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.mediaimpl.jmf.JmfMediaManager;
import org.jivesoftware.smackx.jingle.mediaimpl.test.TestMediaManager;
import org.jivesoftware.smackx.jingle.nat.FixedResolver;
import org.jivesoftware.smackx.jingle.nat.FixedTransportManager;
import org.jivesoftware.smackx.jingle.nat.RTPBridge;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.provider.JingleProvider;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the Jingle extension using the high level API
 * </p>
 *
 * @author Alvaro Saurin
 * @author Thiago Camargo
 */
public class JingleManagerTest extends SmackTestCase {

    private int counter;

    private final Object mutex = new Object();

    /**
     * Constructor for JingleManagerTest.
     *
     * @param name
     */
    public JingleManagerTest(final String name) {
        super(name);

        resetCounter();
    }

    // Counter management

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    public void incCounter() {
        synchronized (mutex) {
            counter++;
        }
    }

    private int valCounter() {
        int val;
        synchronized (mutex) {
            val = counter;
        }
        return val;
    }

    /**
     * Generate a list of payload types
     *
     * @return A testing list
     */
    private List<PayloadType> getTestPayloads1() {
        List<PayloadType> result = new ArrayList<PayloadType>();

        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(36, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(45, "supercodec-4", 1, 98000));

        return result;
    }

    private List<PayloadType> getTestPayloads2() {
        List<PayloadType> result = new ArrayList<PayloadType>();

        result.add(new PayloadType.Audio(27, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(32, "supercodec-4", 1, 98000));
        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));

        return result;
    }

    private List<PayloadType> getTestPayloads3() {
        List<PayloadType> result = new ArrayList<PayloadType>();

        result.add(new PayloadType.Audio(91, "badcodec-1", 2, 28000));
        result.add(new PayloadType.Audio(92, "badcodec-2", 1, 44000));
        result.add(new PayloadType.Audio(93, "badcodec-3", 1, 98000));
        result.add(new PayloadType.Audio(94, "badcodec-4", 2, 14000));

        return result;
    }

    /**
     * Test for the session request detection. Here, we use the same filter we
     * use in the JingleManager...
     */
    public void testInitJingleSessionRequestListeners() {

        resetCounter();

        ProviderManager.getInstance().addIQProvider("jingle", "http://jabber.org/protocol/jingle", new JingleProvider());

        PacketFilter initRequestFilter = new PacketFilter() {
            // Return true if we accept this packet
            public boolean accept(Packet pin) {
                if (pin instanceof IQ) {
                    System.out.println("packet: " + pin.toXML());
                    IQ iq = (IQ) pin;
                    if (iq.getType().equals(IQ.Type.SET)) {
                        System.out.println("packet");
                        if (iq instanceof Jingle) {
                            Jingle jin = (Jingle) pin;
                            if (jin.getAction().equals(JingleActionEnum.SESSION_INITIATE)) {
                                System.out.println("Session initiation packet accepted... ");
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        };

        // Start a packet listener for session initiation requests
        getConnection(0).addPacketListener(new PacketListener() {
            public void processPacket(final Packet packet) {
                System.out.println("Packet detected... ");
                incCounter();
            }
        }, initRequestFilter);

        // Create a dummy packet for testing...
        IQfake iqSent = new IQfake(" <jingle xmlns='http://jabber.org/protocol/jingle'" + " initiator=\"user1@thiago\""
                + " responder=\"user0@thiago\"" + " action=\"session-initiate\" sid=\"08666555\">" + "</jingle>");

        iqSent.setTo(getFullJID(0));
        iqSent.setFrom(getFullJID(0));
        iqSent.setType(IQ.Type.SET);

        System.out.println("Sending packet and waiting... ");
        getConnection(1).sendPacket(iqSent);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        System.out.println("Awake... " + valCounter());
        assertTrue(valCounter() > 0);
    }

    /**
     * High level API test. This is a simple test to use with a XMPP client and
     * check if the client receives the message 1. User_1 will send an
     * invitation to user_2.
     */
    public void testSendSimpleMessage() {

        resetCounter();

        try {
            FixedResolver tr0 = new FixedResolver("127.0.0.1", 54222);
            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
            TestMediaManager tmm0 = new TestMediaManager(ftm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            FixedResolver tr1 = new FixedResolver("127.0.0.1", 54567);
            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
            TestMediaManager tmm1 = new TestMediaManager(ftm1);
            tmm1.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            JingleManager man0 = new JingleManager(getConnection(0), trl0);
            JingleManager man1 = new JingleManager(getConnection(1), trl1);

            // Session 1 waits for connections
            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    incCounter();
                    System.out.println("Session request detected, from " + request.getFrom());
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));
            session0.startOutgoing();

            Thread.sleep(5000);

            assertTrue(valCounter() > 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    /**
     * High level API test. This is a simple test to use with a XMPP client and
     * check if the client receives the message 1. User_1 will send an
     * invitation to user_2.
     */
    public void testAcceptJingleSession() {

        resetCounter();

        try {
            FixedResolver tr0 = new FixedResolver("127.0.0.1", 54222);
            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
            TestMediaManager tmm0 = new TestMediaManager(ftm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            FixedResolver tr1 = new FixedResolver("127.0.0.1", 54567);
            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
            TestMediaManager tmm1 = new TestMediaManager(ftm1);
            tmm1.setPayloads(getTestPayloads2());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            JingleManager man0 = new JingleManager(getConnection(0), trl0);
            JingleManager man1 = new JingleManager(getConnection(1), trl1);
            
            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    incCounter();
                    System.out.println("Session request detected, from " + request.getFrom() + ": accepting.");

                    // We accept the request
                    try {
                        JingleSession session1 = request.accept();
                        session1.startIncoming();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));
            session0.startOutgoing();

            Thread.sleep(20000);

            assertTrue(valCounter() > 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    /**
     * This is a simple test where both endpoints have exactly the same payloads
     * and the session is accepted.
     */
    public void testEqualPayloadsSetSession() {

        resetCounter();

        try {
            FixedResolver tr0 = new FixedResolver("127.0.0.1", 54213);
            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
            TestMediaManager tmm0 = new TestMediaManager(ftm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            FixedResolver tr1 = new FixedResolver("127.0.0.1", 54531);
            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
            TestMediaManager tmm1 = new TestMediaManager(ftm1);
            tmm1.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            JingleManager man0 = new JingleManager(getConnection(0), trl0);
            JingleManager man1 = new JingleManager(getConnection(1), trl1);
            
            man0.addCreationListener(ftm0);
            man1.addCreationListener(ftm1);
            
            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from " + request.getFrom() + ": accepting.");
                    try {
                        // We accept the request
                        JingleSession session1 = request.accept();

                        session1.addListener(new JingleSessionListener() {
                            public void sessionClosed(String reason, JingleSession jingleSession) {
                                System.out.println("sessionClosed().");
                            }

                            public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                                System.out.println("sessionClosedOnError().");
                            }

                            public void sessionDeclined(String reason, JingleSession jingleSession) {
                                System.out.println("sessionDeclined().");
                            }

                            public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                                    JingleSession jingleSession) {
                                incCounter();
                                System.out.println("Responder: the session is fully established.");
                                System.out.println("+ Payload Type: " + pt.getId());
                                System.out.println("+ Local IP/port: " + lc.getIp() + ":" + lc.getPort());
                                System.out.println("+ Remote IP/port: " + rc.getIp() + ":" + rc.getPort());
                            }

                            public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                                // Do Nothing
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }
                        });

                        session1.startIncoming();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request with equal payloads, to " + getFullJID(1) + "...");
            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));
            session0.startOutgoing();

            Thread.sleep(20000);

            assertTrue(valCounter() == 1);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    public void testStagesSession() {

        resetCounter();

        try {
            FixedResolver tr0 = new FixedResolver("127.0.0.1", 54222);
            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
            TestMediaManager tmm0 = new TestMediaManager(ftm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            FixedResolver tr1 = new FixedResolver("127.0.0.1", 54567);
            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
            TestMediaManager tmm1 = new TestMediaManager(ftm1);
            tmm1.setPayloads(getTestPayloads2());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            JingleManager man0 = new JingleManager(getConnection(0), trl0);
            JingleManager man1 = new JingleManager(getConnection(1), trl1);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from " + request.getFrom() + ": accepting.");
                    try {
                        // We accept the request
                        JingleSession session1 = request.accept();

                        session1.addListener(new JingleSessionListener() {
                            public void sessionClosed(String reason, JingleSession jingleSession) {
                                System.out.println("sessionClosed().");
                            }

                            public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                                System.out.println("sessionClosedOnError().");
                            }

                            public void sessionDeclined(String reason, JingleSession jingleSession) {
                                System.out.println("sessionDeclined().");
                            }

                            public void sessionEstablished(PayloadType pt, TransportCandidate rc, final TransportCandidate lc,
                                    JingleSession jingleSession) {
                                incCounter();
                                System.out.println("Responder: the session is fully established.");
                                System.out.println("+ Payload Type: " + pt.getId());
                                System.out.println("+ Local IP/port: " + lc.getIp() + ":" + lc.getPort());
                                System.out.println("+ Remote IP/port: " + rc.getIp() + ":" + rc.getPort());
                            }

                            public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                                // Do Nothing
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }
                        });

                        session1.startIncoming();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                }

                public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                        JingleSession jingleSession) {
                    incCounter();
                    System.out.println("Initiator: the session is fully established.");
                    System.out.println("+ Payload Type: " + pt.getId());
                    System.out.println("+ Local IP/port: " + lc.getIp() + ":" + lc.getPort());
                    System.out.println("+ Remote IP/port: " + rc.getIp() + ":" + rc.getPort());
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }

                public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                    // Do Nothing
                }
            });
            session0.startOutgoing();

            Thread.sleep(20000);

            assertTrue(valCounter() == 2);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    public void testRejectSession() {

        resetCounter();

        try {
            FixedResolver tr0 = new FixedResolver("127.0.0.1", 22222);
            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
            TestMediaManager tmm0 = new TestMediaManager(ftm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            FixedResolver tr1 = new FixedResolver("127.0.0.1", 22444);
            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
            TestMediaManager tmm1 = new TestMediaManager(ftm1);
            tmm1.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            JingleManager man0 = new JingleManager(getConnection(0), trl0);
            JingleManager man1 = new JingleManager(getConnection(1), trl1);
            
            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from " + request.getFrom());

                    // We reject the request
                    try {
                        JingleSession session = request.accept();
                        //session.setInitialSessionRequest(request);
                        session.startIncoming();
                        session.terminate();
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                    incCounter();
                    System.out.println("The session has been closed");
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                    System.out.println("The session has been detected as rejected with reason: " + reason);
                }

                public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                        JingleSession jingleSession) {
                }

                public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                    // Do Nothing
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });

            session0.startOutgoing();

            Thread.sleep(50000);

            //session0.terminate();

            Thread.sleep(10000);

            assertTrue(valCounter() > 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    /**
     * RTP Bridge Test
     */
    public void testRTPBridge() {

        resetCounter();

        try {

            ProviderManager.getInstance().addIQProvider(RTPBridge.NAME, RTPBridge.NAMESPACE, new RTPBridge.Provider());

            RTPBridge response = RTPBridge.getRTPBridge(getConnection(0), "102");

            class Listener implements Runnable {

                private byte[] buf = new byte[5000];
                private DatagramSocket dataSocket;
                private DatagramPacket packet;

                public Listener(DatagramSocket dataSocket) {
                    this.dataSocket = dataSocket;
                }

                public void run() {
                    try {
                        while (true) {
                            // Block until a datagram appears:
                            packet = new DatagramPacket(buf, buf.length);
                            dataSocket.receive(packet);
                            incCounter();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                byte packet[] = { 0, 0, 1, 1, 1, 1, 1 };
                DatagramSocket ds0 = new DatagramSocket(14004, InetAddress.getByName("0.0.0.0"));
                DatagramSocket ds1 = new DatagramSocket(14050, InetAddress.getByName("0.0.0.0"));
                DatagramPacket echo0 = new DatagramPacket(packet, packet.length, InetAddress.getLocalHost(), response.getPortA());
                DatagramPacket echo1 = new DatagramPacket(packet, packet.length, InetAddress.getLocalHost(), response.getPortB());

                ds1.send(echo1);
                ds0.send(echo0);

                Thread.sleep(500);

                Thread t0 = new Thread(new Listener(ds0));
                Thread t1 = new Thread(new Listener(ds1));

                t0.start();
                t1.start();

                int repeat = 300;

                for (int i = 0; i < repeat; i++) {
                    ds0.send(echo0);
                    ds1.send(echo1);
                    Thread.sleep(200);
                }

                System.out.println(valCounter());
                assertTrue(valCounter() == repeat * 2 + 1);

                t0.stop();
                t1.stop();

                ds0.close();
                ds1.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This is a full test in the Jingle API.
     */
    public void testFullTest() {

        resetCounter();
        
        XMPPConnection x0 = getConnection(0);
        XMPPConnection x1 = getConnection(1);

        XMPPConnection.DEBUG_ENABLED = true;

        FixedResolver tr0 = new FixedResolver("127.0.0.1", 20080);
        FixedTransportManager ftm0 = new FixedTransportManager(tr0);
        JmfMediaManager jmf0 = new JmfMediaManager(ftm0);
        List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
        trl0.add(jmf0);

        FixedResolver tr1 = new FixedResolver("127.0.0.1", 20040);
        FixedTransportManager ftm1 = new FixedTransportManager(tr1);
        JmfMediaManager jmf1 = new JmfMediaManager(ftm1);
        List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
        trl1.add(jmf1);

        JingleManager man0 = new JingleManager(x0, trl0);
        JingleManager man1 = new JingleManager(x1, trl1);
        
        man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
            public void sessionRequested(final JingleSessionRequest request) {

                try {

                    JingleSession session = request.accept();
                    session.addListener(new JingleSessionListener() {

                        public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                                JingleSession jingleSession) {
                            incCounter();
                            System.out.println("Establish In");
                        }

                        public void sessionDeclined(String reason, JingleSession jingleSession) {
                        }

                        public void sessionRedirected(String redirection, JingleSession jingleSession) {
                        }

                        public void sessionClosed(String reason, JingleSession jingleSession) {
                            //  incCounter();
                        }

                        public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                            // Do Nothing
                        }

                        public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                            //  incCounter();
                        }
                    });

                    session.startIncoming();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }

            }
        });

        for (int i = 0; i < 3; i++)
            try {

                JingleSession js0 = man0.createOutgoingJingleSession(x1.getUser());

                js0.addListener(new JingleSessionListener() {

                    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                            JingleSession jingleSession) {
                        incCounter();
                        System.out.println("Establish Out");
                    }

                    public void sessionDeclined(String reason, JingleSession jingleSession) {
                    }

                    public void sessionRedirected(String redirection, JingleSession jingleSession) {
                    }

                    public void sessionClosed(String reason, JingleSession jingleSession) {
                        // incCounter();
                    }

                    public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                        // Do Nothing
                    }

                    public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                        //  incCounter();
                    }
                });

                js0.startOutgoing();

                Thread.sleep(8000);
                js0.terminate();

                Thread.sleep(3000);

            } catch (Exception e) {
                e.printStackTrace();
            }

        System.out.println(valCounter());
        assertTrue(valCounter() == 6);
    }

    /**
     * This is a full test in the Jingle API.
     */
    public void testMediaManager() {

        resetCounter();
        
        XMPPConnection x0 = getConnection(0);
        XMPPConnection x1 = getConnection(1);

        FixedResolver tr0 = new FixedResolver("127.0.0.1", 20004);
        FixedTransportManager ftm0 = new FixedTransportManager(tr0);

        FixedResolver tr1 = new FixedResolver("127.0.0.1", 20040);
        FixedTransportManager ftm1 = new FixedTransportManager(tr1);

        try {
            
            JingleMediaManager jingleMediaManager = new JingleMediaManager(ftm0) {
                // Media Session Implementation
                public JingleMediaSession createMediaSession(final PayloadType payloadType, final TransportCandidate remote,
                        final TransportCandidate local, final JingleSession jingleSession) {
                    return new JingleMediaSession(payloadType, remote, local, null, null) {

                        public void initialize() {

                        }

                        public void startTrasmit() {
                            incCounter();
                            System.out.println("Transmit");
                        }

                        public void startReceive() {
                            incCounter();
                            System.out.println("Receive");
                        }

                        public void setTrasmit(boolean active) {
                        }

                        public void stopTrasmit() {
                            incCounter();
                            System.out.println("Stop Transmit");
                        }

                        public void stopReceive() {
                            incCounter();
                            System.out.println("Stop Receive");
                        }
                    };
                }

                public List<PayloadType> getPayloads() {
                    return getTestPayloads1();
                }

                public PayloadType.Audio getPreferredAudioPayloadType() {
                    return (PayloadType.Audio) getTestPayloads1().get(0);
                }

            };
            
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(jingleMediaManager);

            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(jingleMediaManager);

            JingleManager jm0 = new JingleManager(x0, trl0);
            JingleManager jm1 = new JingleManager(x1, trl1);

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

            Thread.sleep(10000);
            js0.terminate();

            Thread.sleep(3000);

            System.out.println(valCounter());

            assertTrue(valCounter() == 8);

            Thread.sleep(15000);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    
//      This test doesn't make sense in light of multiple <content> sections allowed in XEp-166.
//      What we really need is a test for actions: content-add and content-remove.
 
    
//    public void testIncompatibleCodecs() {
//
//        resetCounter();
//
//        try {
//            FixedResolver tr0 = new FixedResolver("127.0.0.1", 54222);
//            FixedTransportManager ftm0 = new FixedTransportManager(tr0);
//            TestMediaManager tmm0 = new TestMediaManager(ftm0);
//            tmm0.setPayloads(getTestPayloads1());
//            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
//            trl0.add(tmm0);
//
//            FixedResolver tr1 = new FixedResolver("127.0.0.1", 54567);
//            FixedTransportManager ftm1 = new FixedTransportManager(tr1);
//            TestMediaManager tmm1 = new TestMediaManager(ftm1);
//            tmm1.setPayloads(getTestPayloads3());
//            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
//            trl1.add(tmm1);
//
//            JingleManager man0 = new JingleManager(getConnection(0), trl0);
//            JingleManager man1 = new JingleManager(getConnection(1), trl1);
//
//            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
//                /**
//                 * Called when a new session request is detected
//                 */
//                public void sessionRequested(final JingleSessionRequest request) {
//                    System.out.println("Session request detected, from " + request.getFrom() + ": accepting.");
//
//                    try {
//                        // We reject the request
//                        JingleSession ses = request.accept();
//
//                        ses.startIncoming();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//
//            // Session 0 starts a request
//            System.out.println("Starting session request, to " + getFullJID(1) + "...");
//            JingleSession session0 = man0.createOutgoingJingleSession(getFullJID(1));
//
//            session0.addListener(new JingleSessionListener() {
//                public void sessionClosed(String reason, JingleSession jingleSession) {
//                    incCounter();
//                }
//
//                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
//                    incCounter();
//                    System.out.println("The session has been close on error with reason: " + e.getMessage());
//                }
//
//                public void sessionDeclined(String reason, JingleSession jingleSession) {
//                    incCounter();
//                    System.out.println("The session has been detected as rejected with reason: " + reason);
//                }
//
//                public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
//                        JingleSession jingleSession) {
//                }
//
//                public void sessionMediaReceived(JingleSession jingleSession, String participant) {
//                    // Do Nothing
//                }
//
//                public void sessionRedirected(String redirection, JingleSession jingleSession) {
//                }
//            });
//
//            session0.startOutgoing();
//
//            Thread.sleep(20000);
//
//            assertTrue(valCounter() > 0);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail("An error occured with Jingle");
//        }
//    }

    protected int getMaxConnections() {
        return 2;
    }

    /**
     * Simple class for testing an IQ...
     *
     * @author Alvaro Saurin
     */
    private class IQfake extends IQ {

        private String s;

        public IQfake(final String s) {
            super();
            this.s = s;
        }

        public String getChildElementXML() {
            StringBuilder buf = new StringBuilder();
            buf.append(s);
            return buf.toString();
        }
    }
}
