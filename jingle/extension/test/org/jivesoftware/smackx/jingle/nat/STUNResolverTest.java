/**
 * $RCSfile$
 * $Revision$
 * $Date$
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
package org.jivesoftware.smackx.jingle.nat;

import de.javawi.jstun.test.demo.StunServer;
import de.javawi.jstun.test.demo.ice.Candidate;
import de.javawi.jstun.test.demo.ice.ICENegociator;
import de.javawi.jstun.util.UtilityException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.mediaimpl.test.TestMediaManager;
import org.jivesoftware.smackx.jingle.nat.STUNResolver.STUNService;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Test the STUN IP resolver.
 *
 * @author Thiago Camargo
 */
public class STUNResolverTest extends SmackTestCase {

    // Counter management

    public STUNResolverTest(final String arg) {
        super(arg);
    }

    private int counter;

    private final Object mutex = new Object();

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    private void incCounter() {
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
     * Test for getPreferredCandidate()
     *
     * @throws Exception
     */
    public void testGetPreferredCandidate() throws Exception {
        int highestPref = 100;

        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 3, 2, "password", 3468, "username1", 1, ICECandidate.Type.prflx);
        TransportCandidate cand2 = new ICECandidate("192.168.5.1", 2, 10, "password", 3469, "username2", 15,
                ICECandidate.Type.prflx);
        TransportCandidate candH = new ICECandidate("192.168.2.11", 1, 2, "password", 3468, "usernameH", highestPref,
                ICECandidate.Type.prflx);
        TransportCandidate cand3 = new ICECandidate("192.168.2.10", 2, 10, "password", 3469, "username3", 2,
                ICECandidate.Type.prflx);
        TransportCandidate cand4 = new ICECandidate("192.168.4.1", 3, 2, "password", 3468, "username4", 78, ICECandidate.Type.prflx);

        STUNResolver stunResolver = new STUNResolver() {
        };
        stunResolver.addCandidate(cand1);
        stunResolver.addCandidate(cand2);
        stunResolver.addCandidate(candH);
        stunResolver.addCandidate(cand3);
        stunResolver.addCandidate(cand4);

        assertEquals(stunResolver.getPreferredCandidate(), candH);
    }

    /**
     * Test for getPreferredCandidate()
     *
     * @throws Exception
     */
    public void testGetPreferredCandidateICE() throws Exception {
        int highestPref = 100;

        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 3, 2, "password", 3468, "username1", 1, ICECandidate.Type.prflx);
        TransportCandidate cand2 = new ICECandidate("192.168.5.1", 2, 10, "password", 3469, "username2", 15,
                ICECandidate.Type.prflx);
        TransportCandidate candH = new ICECandidate("192.168.2.11", 1, 2, "password", 3468, "usernameH", highestPref,
                ICECandidate.Type.prflx);
        TransportCandidate cand3 = new ICECandidate("192.168.2.10", 2, 10, "password", 3469, "username3", 2,
                ICECandidate.Type.prflx);
        TransportCandidate cand4 = new ICECandidate("192.168.4.1", 3, 2, "password", 3468, "username4", 78, ICECandidate.Type.prflx);

        ICEResolver iceResolver = new ICEResolver(getConnection(0), "stun.xten.net", 3478) {
        };
        iceResolver.addCandidate(cand1);
        iceResolver.addCandidate(cand2);
        iceResolver.addCandidate(candH);
        iceResolver.addCandidate(cand3);
        iceResolver.addCandidate(cand4);

        assertEquals(iceResolver.getPreferredCandidate(), candH);
    }

    /**
     * Test priority generated by STUN lib
     *
     * @throws Exception
     */
    public void testICEPriority() throws Exception {

        String first = "";

        for (int i = 0; i < 100; i++) {

            ICENegociator cc = new ICENegociator((short) 1);
            // gather candidates
            cc.gatherCandidateAddresses();
            // priorize candidates
            cc.prioritizeCandidates();
            // get SortedCandidates

            for (Candidate candidate : cc.getSortedCandidates()) {
                short nicNum = 0;
				try {
					Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
					short tempNic = 0;
					NetworkInterface nic = NetworkInterface.getByInetAddress(candidate.getAddress().getInetAddress());
					while(nics.hasMoreElements()) {
						NetworkInterface checkNIC = nics.nextElement();
						if (checkNIC.equals(nic)) {
							nicNum = tempNic;
							break;
						}
						i++;
					}
				} catch (SocketException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
 
                try {
                    TransportCandidate transportCandidate = new ICECandidate(candidate.getAddress().getInetAddress()
                            .getHostAddress(), 1, nicNum, "1", candidate.getPort(), "1", candidate.getPriority(),
                            ICECandidate.Type.prflx);
                    transportCandidate.setLocalIp(candidate.getBase().getAddress().getInetAddress().getHostAddress());
                    System.out.println("C: " + candidate.getAddress().getInetAddress() + "|"
                            + candidate.getBase().getAddress().getInetAddress() + " p:" + candidate.getPriority());
                } catch (UtilityException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            Candidate candidate = cc.getSortedCandidates().get(0);
            String temp = "C: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress()
                    + " p:" + candidate.getPriority();
            if (first.equals(""))
                first = temp;
            assertEquals(first, temp);
            first = temp;
        }
    }

    /**
     * Test for loadSTUNServers()
     *
     * @throws Exception
     */
    public void testLoadSTUNServers() throws Exception {
        STUNResolver stunResolver = new STUNResolver() {
        };
        ArrayList<STUNService> stunServers = stunResolver.loadSTUNServers();

        assertTrue(stunServers.size() > 0);
        System.out.println(stunServers.size() + " servers loaded");
    }

    public void testGetSTUNServer() {

        System.out.println(STUN.serviceAvailable(getConnection(0)));
        STUN stun = STUN.getSTUNServer(getConnection(0));
        for (STUN.StunServerAddress stunServerAddress : stun.getServers())
            System.out.println(stunServerAddress.getServer() + ":" + stunServerAddress.getPort());

        System.out.println(stun.getPublicIp());

    }

    /**
     * Test for resolve()
     *
     * @throws Exception
     */
    public void testResolve() throws Exception {

        final STUNResolver stunResolver = new STUNResolver() {
        };

        stunResolver.addListener(new TransportResolverListener.Resolver() {

            public void candidateAdded(final TransportCandidate cand) {
                incCounter();

                String addr = cand.getIp();
                int port = cand.getPort();

                System.out.println("Addr: " + addr + " port:" + port);

            }

            public void init() {
                System.out.println("Resolution started");
            }

            public void end() {
                System.out.println("Resolution finished");
            }
        });

        try {
            stunResolver.initializeAndWait();
            Thread.sleep(55000);
            assertTrue(valCounter() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a list of payload types
     *
     * @return A testing list
     */
    private ArrayList<PayloadType> getTestPayloads1() {
        ArrayList<PayloadType> result = new ArrayList<PayloadType>();

        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(36, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(45, "supercodec-4", 1, 98000));

        return result;
    }

    private ArrayList<PayloadType> getTestPayloads2() {
        ArrayList<PayloadType> result = new ArrayList<PayloadType>();

        result.add(new PayloadType.Audio(27, "supercodec-3", 2, 28000));
        result.add(new PayloadType.Audio(56, "supercodec-2", 1, 44000));
        result.add(new PayloadType.Audio(32, "supercodec-4", 1, 98000));
        result.add(new PayloadType.Audio(34, "supercodec-1", 2, 14000));

        return result;
    }

    /**
     * This is a simple test where the user_2 rejects the Jingle session.
     */
    public void testSTUNJingleSession() {

        resetCounter();

        try {
            TransportResolver tr1 = new STUNResolver() {
            };
            TransportResolver tr2 = new STUNResolver() {
            };

            // Explicit resolution
            tr1.resolve(null);
            tr2.resolve(null);

            STUNTransportManager stm0 = new STUNTransportManager();
            TestMediaManager tmm0 = new TestMediaManager(stm0);
            tmm0.setPayloads(getTestPayloads1());
            List<JingleMediaManager> trl0 = new ArrayList<JingleMediaManager>();
            trl0.add(tmm0);

            STUNTransportManager stm1 = new STUNTransportManager();
            TestMediaManager tmm1 = new TestMediaManager(stm1);
            tmm1.setPayloads(getTestPayloads2());
            List<JingleMediaManager> trl1 = new ArrayList<JingleMediaManager>();
            trl1.add(tmm1);

            final JingleManager man0 = new JingleManager(getConnection(0), trl0);
            final JingleManager man1 = new JingleManager(getConnection(1), trl1);

            man1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                /**
                 * Called when a new session request is detected
                 */
                public void sessionRequested(final JingleSessionRequest request) {
                    System.out.println("Session request detected, from " + request.getFrom() + ": accepting.");

                    // We accept the request
                    JingleSession session1;
                    try {
                        session1 = request.accept();
                        session1.addListener(new JingleSessionListener() {
                            public void sessionClosed(String reason, JingleSession jingleSession) {
                            }

                            public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
                            }

                            public void sessionDeclined(String reason, JingleSession jingleSession) {
                            }

                            public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc,
                                    JingleSession jingleSession) {
                                incCounter();
                                System.out.println("Responder: the session is fully established.");
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
                        session1.startIncoming();
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

                public void sessionMediaReceived(JingleSession jingleSession, String participant) {
                    // Do Nothing
                }

                public void sessionRedirected(String redirection, JingleSession jingleSession) {
                }
            });
            session0.startOutgoing();

            Thread.sleep(60000);

            assertTrue(valCounter() == 2);

        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occured with Jingle");
        }
    }

    protected int getMaxConnections() {
        return 2;
    }
}
