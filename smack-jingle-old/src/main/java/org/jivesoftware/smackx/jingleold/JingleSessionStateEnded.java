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

import java.util.logging.Logger;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleError;

/**
 * Jingle. 
 *  @author Jeff Williams
 *  @see JingleSessionState
 */
public class JingleSessionStateEnded extends JingleSessionState {

    private static final Logger LOGGER = Logger.getLogger(JingleSessionStateEnded.class.getName());

    private static JingleSessionStateEnded INSTANCE = null;

    protected JingleSessionStateEnded() {
        // Prevent instantiation of the class.
    }

    /**
     *  A thread-safe means of getting the one instance of this class.
     *  @return The singleton instance of this class.
     */
    public synchronized static JingleSessionState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JingleSessionStateEnded();
        }

        return INSTANCE;
    }

    @Override
    public void enter() {
        LOGGER.fine("Session Ended");
        LOGGER.fine("-------------------------------------------------------------------");

    }

    @Override
    public void exit() {
        // TODO Auto-generated method stub

    }

    /**
     * Pretty much nothing is valid for receiving once we've ended the session.
     */
    @Override
    public IQ processJingle(JingleSession session, Jingle jingle, JingleActionEnum action) {
        IQ response = null;

        response = session.createJingleError(jingle, JingleError.MALFORMED_STANZA);

        return response;
    }
}
