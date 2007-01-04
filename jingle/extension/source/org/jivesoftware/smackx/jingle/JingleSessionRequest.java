package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.packet.Jingle;

import java.util.List;

/**
 * A Jingle session request.
 *
 * This class is a facade of a received Jingle request. The user can have direct
 * access to the Jingle packet (<i>JingleSessionRequest.getJingle() </i>) of
 * the request or can use the convencience methods provided by this class.
 *
 * @author Alvaro Saurin
 */
public class JingleSessionRequest {

    private final Jingle jingle; // The Jingle packet

    private final JingleManager manager; // The manager associated to this

    // request

    /**
     * A recieve request is constructed from the Jingle Initiation request
     * received from the initator.
     *
     * @param manager The manager handling this request
     * @param jingle  The jingle IQ recieved from the initiator.
     */
    public JingleSessionRequest(JingleManager manager, Jingle jingle) {
        this.manager = manager;
        this.jingle = jingle;
    }

    /**
     * Returns the fully-qualified jabber ID of the user that requested this
     * session.
     *
     * @return Returns the fully-qualified jabber ID of the user that requested
     *         this session.
     */
    public String getFrom() {
        return jingle.getFrom();
    }

    /**
     * Returns the session ID that uniquely identifies this session.
     *
     * @return Returns the session ID that uniquely identifies this session
     */
    public String getSessionID() {
        return jingle.getSid();
    }

    /**
     * Returns the Jingle packet that was sent by the requestor which contains
     * the parameters of the session.
     */
    public Jingle getJingle() {
        return jingle;
    }

    /**
     * Accepts this request and creates the incoming Jingle session.
     *
     * @param pts list of supported Payload Types
     * @return Returns the <b><i>IncomingJingleSession</b></i> on which the
     *         negotiation can be carried out.
     */
    public synchronized IncomingJingleSession accept(List<PayloadType> pts) throws XMPPException {
        IncomingJingleSession session = null;
        synchronized (manager) {
            session = manager.createIncomingJingleSession(this,
                    pts);
            session.setInitialSessionRequest(this);
        }
        return session;
    }

    /**
     * Accepts this request and creates the incoming Jingle session.
     *
     * @return Returns the <b><i>IncomingJingleSession</b></i> on which the
     *         negotiation can be carried out.
     */
    public synchronized IncomingJingleSession accept() throws XMPPException {
        IncomingJingleSession session = null;
        synchronized (manager) {
            session = manager.createIncomingJingleSession(this);
            session.setInitialSessionRequest(this);
        }
        return session;
    }

    /**
     * Rejects the session request.
     */
    public synchronized void reject() {
        synchronized (manager) {
            manager.rejectIncomingJingleSession(this);
        }
    }
}
