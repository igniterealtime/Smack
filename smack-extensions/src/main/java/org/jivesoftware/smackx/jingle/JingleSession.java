/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.jingle;

import org.jxmpp.jid.Jid;

// TODO: Is this class still required? If not, then remove it.
public class JingleSession {

    private final Jid initiator;

    private final Jid responder;

    private final String sid;

    public JingleSession(Jid initiator, Jid responder, String sid) {
        this.initiator = initiator;
        this.responder = responder;
        this.sid = sid;
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + initiator.hashCode();
        hashCode = 31 * hashCode + responder.hashCode();
        hashCode = 31 * hashCode + sid.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JingleSession)) {
            return false;
        }

        JingleSession otherJingleSession = (JingleSession) other;
        return initiator.equals(otherJingleSession.initiator) && responder.equals(otherJingleSession.responder)
                        && sid.equals(otherJingleSession.sid);
    }
}
