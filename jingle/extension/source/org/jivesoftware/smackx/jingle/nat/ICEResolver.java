/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import de.javawi.jstun.test.demo.ice.Candidate;
import de.javawi.jstun.test.demo.ice.ICENegociator;
import de.javawi.jstun.util.UtilityException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleSession;

import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.List;
import java.util.Random;

/**
 * ICE Resolver for Jingle transport method that results in sending data between two entities using the Interactive Connectivity Establishment (ICE) methodology. (XEP-0176)
 * The goal of this resolver is to make possible to establish and manage out-of-band connections between two XMPP entities, even if they are behind Network Address Translators (NATs) or firewalls.
 * To use this resolver you must have a STUN Server and be in a non STUN blocked network.
 *
 * @author Thiago Camargo
 */
public class ICEResolver extends TransportResolver {

    XMPPConnection connection;
    Random random = new Random();
    long sid;
    String server = "stun.xten.net";
    int port = 3478;
    ICENegociator iceNegociator = null;

    public ICEResolver(XMPPConnection connection, String server, int port) {
        super();
        this.connection = connection;
        this.server = server;
        this.port = port;
        this.setType(Type.ice);
    }

    public void initialize() throws XMPPException {
        if (!isResolving() && !isResolved()) {
            System.out.println("Initialized");

            iceNegociator = new ICENegociator((short) 1, server, port);
            // gather candidates
            iceNegociator.gatherCandidateAddresses();
            // priorize candidates
            iceNegociator.prioritizeCandidates();

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

        for (Candidate candidate : iceNegociator.getSortedCandidates())
            try {
                Candidate.CandidateType type = candidate.getCandidateType();
                String typeString = "local";
                if (type.equals(Candidate.CandidateType.ServerReflexive))
                    typeString = "srflx";
                else if (type.equals(Candidate.CandidateType.PeerReflexive))
                    typeString = "prflx";
                else if (type.equals(Candidate.CandidateType.Relayed))
                    typeString = "relay";
                else
                    typeString = "host";

                TransportCandidate transportCandidate = new ICECandidate(candidate.getAddress().getInetAddress().getHostAddress(), 1, candidate.getNetwork(), String.valueOf(Math.abs(random.nextLong())), candidate.getPort(), "1", candidate.getPriority(), typeString);
                transportCandidate.setLocalIp(candidate.getBase().getAddress().getInetAddress().getHostAddress());
                transportCandidate.setPort(getFreePort());
                try {
                    transportCandidate.addCandidateEcho(session);
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
                this.addCandidate(transportCandidate);

                System.out.println("C: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress() + " p:" + candidate.getPriority());

            }
            catch (UtilityException e) {
                e.printStackTrace();
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

        if (RTPBridge.serviceAvailable(connection)) {
            try {

                String localIp;
                int network;

                if (iceNegociator.getPublicCandidate() != null) {
                    localIp = iceNegociator.getPublicCandidate().getBase().getAddress().getInetAddress().getHostAddress();
                    network = iceNegociator.getPublicCandidate().getNetwork();
                }
                else {
                    localIp = iceNegociator.getSortedCandidates().get(0).getAddress().getInetAddress().getHostAddress();
                    network = iceNegociator.getSortedCandidates().get(0).getNetwork();
                }

                sid = Math.abs(random.nextLong());

                RTPBridge rtpBridge = RTPBridge.getRTPBridge(connection, String.valueOf(sid));

                TransportCandidate localCandidate = new ICECandidate(
                        rtpBridge.getIp(), 1, network, String.valueOf(Math.abs(random.nextLong())), rtpBridge.getPortA(), "1", 0, "relay");
                localCandidate.setLocalIp(localIp);

                TransportCandidate remoteCandidate = new ICECandidate(
                        rtpBridge.getIp(), 1, network, String.valueOf(Math.abs(random.nextLong())), rtpBridge.getPortB(), "1", 0, "relay");
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

            }
            catch (UtilityException e) {
                e.printStackTrace();
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }

        this.setResolveEnd();
    }

}
