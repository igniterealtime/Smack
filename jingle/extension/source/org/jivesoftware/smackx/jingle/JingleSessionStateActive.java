package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;

/**
 *  @author Jeff Williams
 *  @see JingleSessionState
 */
public class JingleSessionStateActive extends JingleSessionState {
    private static JingleSessionStateActive INSTANCE = null;

    protected JingleSessionStateActive() {
        // Prevent instantiation of the class.
    }

    /**
     *  A thread-safe means of getting the one instance of this class.
     *  @return The singleton instance of this class.
     */
    public synchronized static JingleSessionState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JingleSessionStateActive();
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
                break;

            case CONTENT_ADD:
                break;

            case CONTENT_MODIFY:
                break;

            case CONTENT_REMOVE:
                break;

            case SESSION_INFO:
                break;

            case SESSION_TERMINATE:
                receiveSessionTerminateAction(session, jingle);
                break;

            case TRANSPORT_INFO:
                break;

            default:
                // Anything other action is an error.
                response = session.createJingleError(jingle, JingleError.OUT_OF_ORDER);
                break;
        }
        return response;
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
