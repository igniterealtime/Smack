package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;

/**
 * The FixedResolver is a resolver where
 * the external address and port are previously known when the object is
 * initialized.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public class FixedResolver extends TransportResolver {

    TransportCandidate fixedCandidate;

    /**
     * Constructor.
     */
    public FixedResolver(String ip, int port) {
        super();
        setFixedCandidate(ip, port);
    }

    /**
     * Create a basic resolver, where we provide the IP and port.
     *
     * @param ip   an IP address
     * @param port a port
     */
    public void setFixedCandidate(String ip, int port) {
        fixedCandidate = new TransportCandidate.Fixed(ip, port);
    }

    /**
     * Resolve the IP address.
     */
    public synchronized void resolve() throws XMPPException {
        if (!isResolving()) {
            setResolveInit();

            clearCandidates();

            if (fixedCandidate.getLocalIp() == null)
                fixedCandidate.setLocalIp(fixedCandidate.getIp());

            if (fixedCandidate != null) {
                addCandidate(fixedCandidate);
            }

            setResolveEnd();
        }
    }

    /**
     * Initialize the resolver.
     *
     * @throws XMPPException
     */
    public void initialize() throws XMPPException {
        setInitialized();
    }

    public void cancel() throws XMPPException {
        // Nothing to do here
    }
}
