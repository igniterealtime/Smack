package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;

/**
 * Transport manager for Jingle.
 *
 * This class makes easier the use of transport resolvers by presenting a simple
 * interface for algorithm selection. The transport manager also keeps the match
 * between the resolution method and the &lt;transport&gt; element present in
 * Jingle packets.
 *
 * As Jingle have many transport methods (official and unofficial methods),
 * this abstract class helps us to extends the transport support of the API.
 *
 * This class must be used with a JingleManager instance in the following way:
 *
 * JingleManager jingleManager = new JingleManager(xmppConnection, new BasicTransportManager());
 *
 * @author Thiago Camargo
 */
public abstract class JingleTransportManager {
    // This class implements the context of a Strategy pattern...

    /**
     * Deafult contructor.
     */
    public JingleTransportManager() {

    }

    /**
     * Get a new Transport Resolver to be used in a Jingle Session
     *
     * @return
     */
    public TransportResolver getResolver() throws XMPPException {
        TransportResolver resolver = createResolver();
        if (resolver == null) {
            resolver = new BasicResolver();
        }
        resolver.initializeAndWait();

        return resolver;
    }

    /**
     * Create a Transport Resolver instance according to the implementation.
     *
     * @return
     */
    protected abstract TransportResolver createResolver();

}
