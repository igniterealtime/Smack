/**
 * $RCSfile: BridgedResolver.java,v $
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

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

/**
 * Bridged Resolver use a RTPBridge Service to add a relayed candidate.
 * A very reliable solution for NAT Traversal.
 * <p/>
 * The resolver verify is the XMPP Server that the client is connected offer this service.
 * If the server supports, a candidate is requested from the service.
 * The resolver adds this candidate
 */
public class BridgedResolver extends TransportResolver {

    Connection connection;

    Random random = new Random();

    long sid;

    /**
     * Constructor.
     * A Bridged Resolver need a Connection to connect to a RTP Bridge.
     */
    public BridgedResolver(Connection connection) {
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

        String localIp = getLocalHost();

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

    public static String getLocalHost() {
        Enumeration<NetworkInterface> ifaces = null;

        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e) {
            e.printStackTrace();
        }

        while (ifaces.hasMoreElements()) {

            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();

            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (!iaddress.isLoopbackAddress() && !iaddress.isLinkLocalAddress() && !iaddress.isSiteLocalAddress() && !(iaddress instanceof Inet6Address)) {
                    return iaddress.getHostAddress();
                }
            }
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return "127.0.0.1";

    }

}
