/**
 * $RCSfile$
 * $Revision: 7329 $
 * $Date: 2007-02-28 20:59:28 -0300 (qua, 28 fev 2007) $
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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;

import java.util.Random;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Bridged Resolver use a RTPBridge Service to add a relayed candidate.
 * A very reliable solution for NAT Traversal.
 *
 * The resolver verify is the XMPP Server that the client is connected offer this service.
 * If the server supports, a candidate is requested from the service.
 * The resolver adds this candidate
 */
public class BridgedResolver extends TransportResolver{

    XMPPConnection connection;

    Random random = new Random();

    long sid;

    /**
     * Constructor.
     * A Bridged Resolver need a XMPPConnection to connect to a RTP Bridge.
     */
    public BridgedResolver(XMPPConnection connection) {
        super();
        this.connection = connection;
    }

    /**
     * Resolve Bridged Candidate.
     * <p/>
     * The BridgedResolver takes the IP addresse and ports of a jmf proxy service.
     */
    public synchronized void resolve(JingleSession session) throws XMPPException {

        setResolveInit();

        clearCandidates();

        sid = Math.abs(random.nextLong());

        RTPBridge rtpBridge = RTPBridge.getRTPBridge(connection, String.valueOf(sid));


        String localIp="127.0.0.1";
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();

             InetAddress    iaddress = InetAddress.getLocalHost();

            System.out.println(iaddress.isLoopbackAddress());
            System.out.println(iaddress.isLinkLocalAddress());
            System.out.println(iaddress.isSiteLocalAddress());

        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

        TransportCandidate localCandidate = new TransportCandidate.Fixed(
                rtpBridge.getIp(), rtpBridge.getPortA());
        localCandidate.setLocalIp(localIp);

        TransportCandidate remoteCandidate = new TransportCandidate.Fixed(
                rtpBridge.getIp(), rtpBridge.getPortB());
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

        setResolveEnd();
    }

    public void initialize() throws XMPPException {

        clearCandidates();

        if (!RTPBridge.serviceAvailable(connection)) {
            setInitialized();
            throw new XMPPException("No RTP Bridge service available");
        }
        setInitialized();

    }

    public void cancel() throws XMPPException {
        // Nothing to do here
    }

}
