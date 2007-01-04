package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;

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
    public synchronized void resolve() throws XMPPException {

        setResolveInit();

        clearCandidates();

        Enumeration ifaces = null;

        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (ifaces.hasMoreElements()) {

            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
            Enumeration iaddresses = iface.getInetAddresses();
            
            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = (InetAddress) iaddresses.nextElement();
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

            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
            Enumeration iaddresses = iface.getInetAddresses();

            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = (InetAddress) iaddresses.nextElement();
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
