/**
 *
 * Copyright 2003-2005 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingleold.nat;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic Resolver takes all IP addresses of the interfaces and uses the
 * first non-loopback address.
 * A very simple and easy to use resolver.
 */
public class BasicResolver extends TransportResolver {
    private static final Logger LOGGER = Logger.getLogger(BasicResolver.class.getName());

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
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    @Override
    public synchronized void resolve(JingleSession session) throws XMPPException, NotConnectedException, InterruptedException {

        setResolveInit();

        clearCandidates();

        Enumeration<NetworkInterface> ifaces = null;

        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            LOGGER.log(Level.WARNING, "exception", e);
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
            LOGGER.log(Level.WARNING, "exception", e);
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
            LOGGER.log(Level.WARNING, "exception", e);
        }
        setResolveEnd();

    }

    @Override
    public void initialize() throws XMPPException {
        setInitialized();
    }

    @Override
    public void cancel() throws XMPPException {
        // Nothing to do here
    }
}
