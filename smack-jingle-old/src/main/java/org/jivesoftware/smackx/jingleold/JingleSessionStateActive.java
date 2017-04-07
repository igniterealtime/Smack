/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.jingleold;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleError;

/**
 * Jingle. 
 *  @author Jeff Williams
 *  @see JingleSessionState
 */
public class JingleSessionStateActive extends JingleSessionState {
    private static final Logger LOGGER = Logger.getLogger(JingleSessionStateActive.class.getName());

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

    @Override
    public void enter() {
        // TODO Auto-generated method stub

    }

    @Override
    public void exit() {
        // TODO Auto-generated method stub

    }

    @Override
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }

        return response;
    }

}
