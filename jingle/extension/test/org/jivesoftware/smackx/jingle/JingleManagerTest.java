/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
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
import org.jivesoftware.smackx.jingle.nat.*;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.provider.JingleProvider;
import org.jivesoftware.jingleaudio.jmf.JmfMediaManager;

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
    private ArrayList getTestPayloads1() {
        ArrayList result = new ArrayList();

        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(36, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(45, "supercodec-4", 1, 98000));

        return result;
    }

    private ArrayList getTestPayloads2() {
        ArrayList result = new ArrayList();

        result.add(new PayloadType.Audio(27, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(32, "supercodec-4", 1, 98000));
        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));

        return result;
    }

    private ArrayList getTestPayloads3() {
        ArrayList result = new ArrayList();

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

        ProviderManager.getInstance().addIQProvider("jingle",
                "http://jabber.org/protocol/jingle",
                new JingleProvider());

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
                            if (jin.getAction().equals(Jingle.Action.SESSIONINITIATE)) {
                                System.out
                                        .println("Session initiation packet accepted... ");
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
        IQfake iqSent = new IQfake(
                " <jingle xmlns='http://jabber.org/protocol/jingle'"
                        + " initiator=\"user1@thiago\""
                        + " responder=\"user0@thiago\""
                        + " action=\"session-initiate\" sid=\"08666555\">"
                        + "</jingle>");

        iqSent.setTo(getFullJID(0));
        iqSent.setFrom(getFullJID(0));
        iqSent.setType(IQ.Type.SET);

        System.out.println("Sending packet and waiting... ");
        getConnection(1).sendPacket(iqSent);
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
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
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 54222);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 54567);

            JingleManager man0 = new JingleManager(getConnection(0), tr1);
            JingleManager man1 = new JingleManager(getConnection(1), tr2);

            // Session 1 waits for connections
            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    incCounter();
                    System.out.println("Session request detected, from "
                            + request.getFrom());
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());
            session0.start(null);

            Thread.sleep(5000);

            assertTrue(valCounter() > 0);

        }
        catch (Exception e) {
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
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 54222);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 54567);

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    incCounter();
                    System.out.println("Session request detected, from "
                            + request.getFrom() + ": accepting.");

                    // We accept the request
                    try {
                        IncomingJingleSession session1 = request.accept(getTestPayloads2());
                        session1.start(request);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());
            session0.start(null);

            Thread.sleep(20000);

            assertTrue(valCounter() > 0);

        }
        catch (Exception e) {
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
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 54213);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 54531);

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from "
                            + request.getFrom() + ": accepting.");
                    try {
                        // We accept the request
                        IncomingJingleSession session1 = request.accept(getTestPayloads1());

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

                            public void sessionEstablished(PayloadType pt,
                                    TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                                incCounter();
                                System.out
                                        .println("Responder: the session is fully established.");
                                System.out.println("+ Payload Type: " + pt.getId());
                                System.out.println("+ Local IP/port: " + lc.getIp() + ":"
                                        + lc.getPort());
                                System.out.println("+ Remote IP/port: " + rc.getIp() + ":"
                                        + rc.getPort());
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }
                        });

                        session1.start(request);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request with equal payloads, to "
                    + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());
            session0.start(null);

            Thread.sleep(20000);

            assertTrue(valCounter() == 1);

        }
        catch (Exception e) {
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
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 54222);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 54567);

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from "
                            + request.getFrom() + ": accepting.");
                    try {
                        // We accept the request
                        IncomingJingleSession session1 = request.accept(getTestPayloads2());

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

                            public void sessionEstablished(PayloadType pt,
                                    TransportCandidate rc, final TransportCandidate lc, JingleSession jingleSession) {
                                incCounter();
                                System.out
                                        .println("Responder: the session is fully established.");
                                System.out.println("+ Payload Type: " + pt.getId());
                                System.out.println("+ Local IP/port: " + lc.getIp() + ":"
                                        + lc.getPort());
                                System.out.println("+ Remote IP/port: " + rc.getIp() + ":"
                                        + rc.getPort());
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }
                        });


                        session1.start(request);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                }

                public void sessionEstablished(PayloadType pt,
                        TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                    incCounter();
                    System.out.println("Initiator: the session is fully established.");
                    System.out.println("+ Payload Type: " + pt.getId());
                    System.out.println("+ Local IP/port: " + lc.getIp() + ":"
                            + lc.getPort());
                    System.out.println("+ Remote IP/port: " + rc.getIp() + ":"
                            + rc.getPort());
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });
            session0.start(null);

            Thread.sleep(20000);

            assertTrue(valCounter() == 2);

        }
        catch (Exception e) {
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
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 22222);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 22444);

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from "
                            + request.getFrom());

                    // We reject the request
                    try {
                        IncomingJingleSession session = request.accept(getTestPayloads1());
                        session.setInitialSessionRequest(request);
                        session.start();
                        session.terminate();
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                    System.out.println("The session has been closed");
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                    incCounter();
                    System.out
                            .println("The session has been detected as rejected with reason: "
                                    + reason);
                }

                public void sessionEstablished(PayloadType pt,
                        TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });

            session0.start();

            Thread.sleep(50000);

            //session0.terminate();

            Thread.sleep(10000);

            assertTrue(valCounter() > 0);

        }
        catch (Exception e) {
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

            ProviderManager.getInstance().addIQProvider(RTPBridge.NAME,
                    RTPBridge.NAMESPACE, new RTPBridge.Provider());

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
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                byte packet[] = {0, 0, 1, 1, 1, 1, 1};
                DatagramSocket ds0 = new DatagramSocket(14004, InetAddress.getByName("0.0.0.0"));
                DatagramSocket ds1 = new DatagramSocket(14050, InetAddress.getByName("0.0.0.0"));
                DatagramPacket echo0 = new DatagramPacket(packet, packet.length,
                        InetAddress.getLocalHost(), response.getPortA());
                DatagramPacket echo1 = new DatagramPacket(packet, packet.length,
                        InetAddress.getLocalHost(), response.getPortB());

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

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        }

    }

    /**
     * This is a full test in the Jingle API.
     */
    public void testFullTest() {

        resetCounter();

        try {

            XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(
                    x0, new STUNResolver() {
            });
            final JingleManager jm1 = new JingleManager(
                    x1, new FixedResolver("127.0.0.1", 20040));

//            JingleManager jm0 = new JingleSessionManager(
//                    x0, new ICEResolver());
//            JingleManager jm1 = new JingleSessionManager(
//                    x1, new ICEResolver());

            JingleMediaManager jingleMediaManager = new JmfMediaManager();

            jm0.setMediaManager(jingleMediaManager);
            jm1.setMediaManager(jingleMediaManager);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());
                        session.addListener(new JingleSessionListener() {

                            public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                                incCounter();
                                System.out.println("Establish In");
                            }

                            public void sessionDeclined(String reason, JingleSession jingleSession) {
                            }

                            public void sessionRedirected(String redirection, JingleSession jingleSession) {
                            }

                            public void sessionClosed(String reason, JingleSession jingleSession) {
                            }

                            public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                            }
                        });

                        session.start();
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession(x1.getUser());

            js0.addListener(new JingleSessionListener() {

                public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    incCounter();
                    System.out.println("Establish Out");
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void sessionClosed(String reason, JingleSession jingleSession) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });

            js0.start();

            Thread.sleep(12000);
            js0.terminate();

            assertTrue(valCounter() == 2);
            //Thread.sleep(15000);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This is a full test in the Jingle API.
     */
    public void testMediaManager() {

        resetCounter();

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = getConnection(0);
            XMPPConnection x1 = getConnection(1);

            final JingleManager jm0 = new JingleManager(
                    x0, new FixedResolver("127.0.0.1", 20004));
            final JingleManager jm1 = new JingleManager(
                    x1, new FixedResolver("127.0.0.1", 20040));

            //JingleManager jm0 = new ICETransportManager(x0, "stun.xten.net", 3478);
            //JingleManager jm1 = new ICETransportManager(x1, "stun.xten.net", 3478);

            JingleMediaManager jingleMediaManager = new JingleMediaManager() {
                // Media Session Implementation
                public JingleMediaSession createMediaSession(final PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local) {
                    return new JingleMediaSession(payloadType, remote, local) {

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
                    return new ArrayList();
                }

                public PayloadType.Audio getPreferredAudioPayloadType() {
                    return null;
                }


            };

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

            Thread.sleep(10000);
            js0.terminate();

            Thread.sleep(3000);

            System.out.println(valCounter());

            assertTrue(valCounter() == 8);
            //Thread.sleep(15000);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    public void testIncompatibleCodecs() {

        resetCounter();

        try {
            TransportResolver tr1 = new FixedResolver("127.0.0.1", 54222);
            TransportResolver tr2 = new FixedResolver("127.0.0.1", 54567);

            final JingleManager man0 = new JingleManager(getConnection(0), tr1);
            final JingleManager man1 = new JingleManager(getConnection(1), tr2);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested
                        (
                                final JingleSessionRequest request) {
                    System.out.println("Session request detected, from "
                            + request.getFrom() + ": accepting.");

                    try {
                        // We reject the request
                        IncomingJingleSession ses = request.accept(getTestPayloads3());

                        ses.start(request);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Session 0 starts a request
            System.out.println("Starting session request, to " + getFullJID(1) + "...");
            OutgoingJingleSession session0 = man0.createOutgoingJingleSession(
                    getFullJID(1), getTestPayloads1());

            session0.addListener(new JingleSessionListener() {
                public void sessionClosed(String reason, JingleSession jingleSession) {
                }

                public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                    incCounter();
                    System.out
                            .println("The session has been close on error with reason: "
                                    + e.getMessage());
                }

                public void sessionDeclined(String reason, JingleSession jingleSession) {
                    incCounter();
                    System.out
                            .println("The session has been detected as rejected with reason: "
                                    + reason);
                }

                public void sessionEstablished(PayloadType pt,
                        TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });

            session0.start(null);

            Thread.sleep(20000);

            assertTrue(valCounter() > 0);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

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
            StringBuffer buf = new StringBuffer();
            buf.append(s);
            return buf.toString();
        }
    }
}
