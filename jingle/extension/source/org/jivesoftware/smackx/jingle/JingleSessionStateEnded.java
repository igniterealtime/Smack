/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;

/**
 *  @author Jeff Williams
 *  @see JingleSessionState
 */
public class JingleSessionStateEnded extends JingleSessionState {
	
	private static final SmackLogger LOGGER = SmackLogger.getLogger(JingleSessionStateEnded.class);

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

    public void enter() {
        LOGGER.debug("Session Ended");
        LOGGER.debug("-------------------------------------------------------------------");

    }

    public void exit() {
        // TODO Auto-generated method stub

    }

    /**
     * Pretty much nothing is valid for receiving once we've ended the session.
     */
    public IQ processJingle(JingleSession session, Jingle jingle, JingleActionEnum action) {
        IQ response = null;
        
        response = session.createJingleError(jingle, JingleError.MALFORMED_STANZA);

        return response;
    }
}
