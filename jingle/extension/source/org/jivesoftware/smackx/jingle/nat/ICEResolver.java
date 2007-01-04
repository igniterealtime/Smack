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

import java.net.UnknownHostException;
import java.util.List;

/**
 * ICE Resolver for Jingle transport method that results in sending data between two entities using the Interactive Connectivity Establishment (ICE) methodology. (XEP-0176)
 * The goal of this resolver is to make possible to establish and manage out-of-band connections between two XMPP entities, even if they are behind Network Address Translators (NATs) or firewalls.
 * To use this resolver you must have a STUN Server and be in a non STUN blocked network.
 *
 * @author Thiago Camargo
 */
public class ICEResolver extends TransportResolver {

    public ICEResolver() {
        super();
        this.setType(Type.ice);
    }

    public void initialize() throws XMPPException {
        if (!isResolving() && !isResolved()) {
            System.out.println("Initialized");
            
            ICENegociator cc = new ICENegociator((short) 1);
            // gather candidates
            cc.gatherCandidateAddresses();
            // priorize candidates
            cc.prioritizeCandidates();
            // get SortedCandidates
            //List<Candidate> sortedCandidates = cc.getSortedCandidates();

            for (Candidate candidate : cc.getSortedCandidates())
                try {
                    TransportCandidate transportCandidate = new TransportCandidate.Ice(candidate.getAddress().getInetAddress().getHostAddress(), 1, candidate.getNetwork(), "1", candidate.getPort(), "1", candidate.getPriority());
                    transportCandidate.setLocalIp(candidate.getBase().getAddress().getInetAddress().getHostAddress());
                    this.addCandidate(transportCandidate);
                    System.out.println("C: " + candidate.getAddress().getInetAddress() + "|" + candidate.getBase().getAddress().getInetAddress() + " p:" + candidate.getPriority());
                } catch (UtilityException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
        }
        this.setInitialized();
    }

    public void cancel() throws XMPPException {

    }

    /**
     * Resolve the IP and obtain a valid transport method.
     */
    public synchronized void resolve() throws XMPPException {
        this.setResolveInit();
        System.out.println("Resolve");
        this.setResolveEnd();
    }

}
