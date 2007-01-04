package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import java.util.Random;

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
    public synchronized void resolve() throws XMPPException {

        setResolveInit();

        clearCandidates();

        sid = Math.abs(random.nextLong());

        RTPBridge rtpBridge = RTPBridge.getRTPBridge(connection, String.valueOf(sid));

        BasicResolver basicResolver = new BasicResolver();

        basicResolver.initializeAndWait();
        basicResolver.resolve();

        TransportCandidate localCandidate = new TransportCandidate.Fixed(
                rtpBridge.getIp(), rtpBridge.getPortA());
        localCandidate.setLocalIp(basicResolver.getCandidate(0).getLocalIp());

        TransportCandidate remoteCandidate = new TransportCandidate.Fixed(
                rtpBridge.getIp(), rtpBridge.getPortB());
        remoteCandidate.setLocalIp(basicResolver.getCandidate(0).getLocalIp());

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
