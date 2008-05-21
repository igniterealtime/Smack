package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.Jingle;

/**
 *  @author Jeff Williams
 *  @see JingleSessionState
 */

public class JingleSessionStatePending extends JingleSessionState {
    private static JingleSessionStatePending INSTANCE = null;

    protected JingleSessionStatePending() {
        // Prevent instantiation of the class.
    }

    /**
     *  A thread-safe means of getting the one instance of this class.
     *  @return The singleton instance of this class.
     */
    public synchronized static JingleSessionState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JingleSessionStatePending();
        }
        return INSTANCE;
    }

    public void enter() {
        // TODO Auto-generated method stub

    }

    public void exit() {
        // TODO Auto-generated method stub

    }

    public IQ processJingle(JingleSession session, Jingle jingle, JingleActionEnum action) {
        IQ response = null;

        switch (action) {

            case CONTENT_ACCEPT:
                response = receiveContentAcceptAction(jingle);
                break;

            case CONTENT_MODIFY:
                break;

            case CONTENT_REMOVE:
                break;

            case SESSION_ACCEPT:
                response = receiveSessionAcceptAction(session, jingle);
                break;

            case SESSION_INFO:
                break;

            case SESSION_TERMINATE:
                response = receiveSessionTerminateAction(session, jingle);
                break;

            case TRANSPORT_INFO:
                break;

            default:
                // Anything other action is an error.
                //response = createJingleError(inJingle, JingleError.OUT_OF_ORDER);
                break;
        }

        return response;
    }

    /**
     * Receive and process the <session-accept> action.
     */
    private IQ receiveContentAcceptAction(Jingle inJingle) {

        // According to XEP-167 the only thing we can do is ack.
        //setSessionState(JingleSessionStateEnum.ACTIVE);
        //return createAck(inJingle);

        // This is now handled by the media negotiator for the matching <content> segment.
        return null;
    }

    /**
     * Receive and process the <session-accept> action.
     */
    private IQ receiveSessionAcceptAction(JingleSession session, Jingle inJingle) {

        // According to XEP-166 the only thing we can do is ack.
        session.setSessionState(JingleSessionStateActive.getInstance());
        return session.createAck(inJingle);
    }

    /**
     * Receive and process the <session-terminate> action.
     */
    private IQ receiveSessionTerminateAction(JingleSession session, Jingle jingle) {

        // According to XEP-166 the only thing we can do is ack.
        IQ response = session.createAck(jingle);

        try {
            session.terminate("Closed remotely");
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        return response;
    }
}
