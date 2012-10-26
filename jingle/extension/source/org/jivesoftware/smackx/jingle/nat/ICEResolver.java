/**
 * $RCSfile: ICEResolver.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright 2003-2005 Jive Software.
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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.SmackLogger;

import de.javawi.jstun.test.demo.ice.Candidate;
import de.javawi.jstun.test.demo.ice.ICENegociator;
import de.javawi.jstun.util.UtilityException;

/**
 * ICE Resolver for Jingle transport method that results in sending data between two entities using the Interactive Connectivity Establishment (ICE) methodology. (XEP-0176)
 * The goal of this resolver is to make possible to establish and manage out-of-band connections between two XMPP entities, even if they are behind Network Address Translators (NATs) or firewalls.
 * To use this resolver you must have a STUN Server and be in a non STUN blocked network. Or use a XMPP server with public IP detection Service.
 *
 * @author Thiago Camargo
 */
public class ICEResolver extends TransportResolver {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(ICEResolver.class);

    Connection connection;
    Random random = new Random();
    long sid;
    String server;
    int port;
    static Map<String, ICENegociator> negociatorsMap = new HashMap<String, ICENegociator>();
    //ICENegociator iceNegociator = null;

    public ICEResolver(Connection connection, String server, int port) {
        super();
        this.connection = connection;
        this.server = server;
        this.port = port;
        this.setType(Type.ice);
    }

    public void initialize() throws XMPPException {
        if (!isResolving() && !isResolved()) {
            LOGGER.debug("Initialized");

            // Negotiation with a STUN server for a set of interfaces is quite slow, but the results
            // never change over then instance of a JVM.  To increase connection performance considerably
            // we now cache established/initialized negotiators for each STUN server, so that subsequent uses
            // of the STUN server are much, much faster.
            if (negociatorsMap.get(server) == null) {
            	ICENegociator iceNegociator = new ICENegociator(server, port, (short) 1);
            	negociatorsMap.put(server, iceNegociator);
            	
            	// gather candidates
            	iceNegociator.gatherCandidateAddresses();
            	// priorize candidates
            	iceNegociator.prioritizeCandidates();
            }

        }
        this.setInitialized();
    }

    public void cancel() throws XMPPException {

    }

    /**
     * Resolve the IP and obtain a valid transport method.
     */
    public synchronized void resolve(JingleSession session) throws XMPPException {
        this.setResolveInit();

        for (TransportCandidate candidate : this.getCandidatesList()) {
            if (candidate instanceof ICECandidate) {
                ICECandidate iceCandidate = (ICECandidate) candidate;
                iceCandidate.removeCandidateEcho();
            }
        }

        this.clear();

        // Create a transport candidate for each ICE negotiator candidate we have.
        ICENegociator iceNegociator = negociatorsMap.get(server);
        for (Candidate candidate : iceNegociator.getSortedCandidates())
            try {
                Candidate.CandidateType type = candidate.getCandidateType();
                ICECandidate.Type iceType = ICECandidate.Type.local;
                if (type.equals(Candidate.CandidateType.ServerReflexive))
                    iceType = ICECandidate.Type.srflx;
                else if (type.equals(Candidate.CandidateType.PeerReflexive))
                    iceType = ICECandidate.Type.prflx;
                else if (type.equals(Candidate.CandidateType.Relayed))
                    iceType = ICECandidate.Type.relay;
                else
                    iceType = ICECandidate.Type.host;

               // JBW/GW - 17JUL08: Figure out the zero-based NIC number for this candidate.
                short nicNum = 0;
				try {
					Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
					short i = 0;
					NetworkInterface nic = NetworkInterface.getByInetAddress(candidate.getAddress().getInetAddress());
					while(nics.hasMoreElements()) {
						NetworkInterface checkNIC = nics.nextElement();
						if (checkNIC.equals(nic)) {
							nicNum = i;
							break;
						}
						i++;
					}
				} catch (SocketException e1) {
					e1.printStackTrace();
				}
                
                TransportCandidate transportCandidate = new ICECandidate(candidate.getAddress().getInetAddress().getHostAddress(), 1, nicNum, String.valueOf(Math.abs(random.nextLong())), candidate.getPort(), "1", candidate.getPriority(), iceType);
                transportCandidate.setLocalIp(candidate.getBase().getAddress().getInetAddress().getHostAddress());
                transportCandidate.setPort(getFreePort());
                try {
                    transportCandidate.addCandidateEcho(session);
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
                this.addCandidate(transportCandidate);

                LOGGER.debug("Candidate addr: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress() + " Priority:" + candidate.getPriority());

            }
            catch (UtilityException e) {
                e.printStackTrace();
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

        // Get a Relay Candidate from XMPP Server

        if (RTPBridge.serviceAvailable(connection)) {
//            try {

                String localIp;
                int network;
                
                
                // JBW/GW - 17JUL08: ICENegotiator.getPublicCandidate() always returned null in JSTUN 1.7.0, and now the API doesn't exist in JSTUN 1.7.1
//                if (iceNegociator.getPublicCandidate() != null) {
//                    localIp = iceNegociator.getPublicCandidate().getBase().getAddress().getInetAddress().getHostAddress();
//                    network = iceNegociator.getPublicCandidate().getNetwork();
//                }
//                else {
                {
                    localIp = BridgedResolver.getLocalHost();
                    network = 0;
                }

                sid = Math.abs(random.nextLong());

                RTPBridge rtpBridge = RTPBridge.getRTPBridge(connection, String.valueOf(sid));

                TransportCandidate localCandidate = new ICECandidate(
                        rtpBridge.getIp(), 1, network, String.valueOf(Math.abs(random.nextLong())), rtpBridge.getPortA(), "1", 0, ICECandidate.Type.relay);
                localCandidate.setLocalIp(localIp);

                TransportCandidate remoteCandidate = new ICECandidate(
                        rtpBridge.getIp(), 1, network, String.valueOf(Math.abs(random.nextLong())), rtpBridge.getPortB(), "1", 0, ICECandidate.Type.relay);
                remoteCandidate.setLocalIp(localIp);

                localCandidate.setSymmetric(remoteCandidate);
                remoteCandidate.setSymmetric(localCandidate);

                localCandidate.setPassword(rtpBridge.getPass());
                remoteCandidate.setPassword(rtpBridge.getPass());

                localCandidate.setSessionId(rtpBridge.getSid());
                remoteCandidate.setSessionId(rtpBridge.getSid());

                localCandidate.setConnection(this.connection);
                remoteCandidate.setConnection(this.connection);

                addCandidate(localCandidate);

//            }
//            catch (UtilityException e) {
//                e.printStackTrace();
//            }
//            catch (UnknownHostException e) {
//                e.printStackTrace();
//            }

            // Get Public Candidate From XMPP Server

 // JBW/GW - 17JUL08 - ICENegotiator.getPublicCandidate() always returned null in JSTUN 1.7.0, and now it doesn't exist in JSTUN 1.7.1
 //          if (iceNegociator.getPublicCandidate() == null) {
            if (true) {

                String publicIp = RTPBridge.getPublicIP(connection);

                if (publicIp != null && !publicIp.equals("")) {

                    Enumeration<NetworkInterface> ifaces = null;

                    try {
                        ifaces = NetworkInterface.getNetworkInterfaces();
                    }
                    catch (SocketException e) {
                        e.printStackTrace();
                    }

                    // If detect this address in local machine, don't use it.

                    boolean found = false;

                    while (ifaces.hasMoreElements() && !false) {

                        NetworkInterface iface = ifaces.nextElement();
                        Enumeration<InetAddress> iaddresses = iface.getInetAddresses();

                        while (iaddresses.hasMoreElements()) {
                            InetAddress iaddress = iaddresses.nextElement();
                            if (iaddress.getHostAddress().indexOf(publicIp) > -1) {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        try {
                            TransportCandidate publicCandidate = new ICECandidate(
                                    publicIp, 1, 0, String.valueOf(Math.abs(random.nextLong())), getFreePort(), "1", 0, ICECandidate.Type.srflx);
                            publicCandidate.setLocalIp(InetAddress.getLocalHost().getHostAddress());

                            try {
                                publicCandidate.addCandidateEcho(session);
                            }
                            catch (SocketException e) {
                                e.printStackTrace();
                            }

                            addCandidate(publicCandidate);
                        }
                        catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

        this.setResolveEnd();
    }

}
