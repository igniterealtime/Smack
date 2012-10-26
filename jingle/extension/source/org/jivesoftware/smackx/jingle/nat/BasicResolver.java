/**
 * $RCSfile: BasicResolver.java,v $
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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Basic Resolver takes all IP addresses of the interfaces and uses the
 * first non-loopback address.
 * A very simple and easy to use resolver.
 */
public class BasicResolver extends TransportResolver {

    /**
     * Constructor.
     */
    public BasicResolver() {
        super();
    }

    /**
     * Resolve the IP address.
     * <p/>
     * The BasicResolver takes the IP addresses of the interfaces and uses the
     * first non-loopback, non-linklocal and non-sitelocal address.
     */
    public synchronized void resolve(JingleSession session) throws XMPPException {

        setResolveInit();

        clearCandidates();

        Enumeration<NetworkInterface> ifaces = null;

        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (ifaces.hasMoreElements()) {

            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
            
            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (!iaddress.isLoopbackAddress() && !iaddress.isLinkLocalAddress() && !iaddress.isSiteLocalAddress()) {
                    TransportCandidate tr = new TransportCandidate.Fixed(iaddress.getHostAddress() != null ? iaddress.getHostAddress() : iaddress.getHostName(), getFreePort());
                    tr.setLocalIp(iaddress.getHostAddress() != null ? iaddress.getHostAddress() : iaddress.getHostName());
                    addCandidate(tr);
                    setResolveEnd();
                    return;
                }
            }
        }

        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (ifaces.hasMoreElements()) {

            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();

            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (!iaddress.isLoopbackAddress() && !iaddress.isLinkLocalAddress()) {
                    TransportCandidate tr = new TransportCandidate.Fixed(iaddress.getHostAddress() != null ? iaddress.getHostAddress() : iaddress.getHostName(), getFreePort());
                    tr.setLocalIp(iaddress.getHostAddress() != null ? iaddress.getHostAddress() : iaddress.getHostName());
                    addCandidate(tr);
                    setResolveEnd();
                    return;
                }
            }
        }

        try {
            TransportCandidate tr = new TransportCandidate.Fixed(InetAddress.getLocalHost().getHostAddress() != null ? InetAddress.getLocalHost().getHostAddress() : InetAddress.getLocalHost().getHostName(), getFreePort());
            tr.setLocalIp(InetAddress.getLocalHost().getHostAddress() != null ? InetAddress.getLocalHost().getHostAddress() : InetAddress.getLocalHost().getHostName());
            addCandidate(tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setResolveEnd();

    }

    public void initialize() throws XMPPException {
        setInitialized();
    }

    public void cancel() throws XMPPException {
        // Nothing to do here
    }
}
