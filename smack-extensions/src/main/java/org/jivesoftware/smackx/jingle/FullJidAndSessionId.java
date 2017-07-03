/**
 *
 * Copyright 2017 Florian Schmaus, Paul Schaub
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

import org.jxmpp.jid.FullJid;

/**
 * Pair of jid and sessionId.
 */
public class FullJidAndSessionId {
    private final FullJid fullJid;
    private final String sessionId;

    public FullJidAndSessionId(FullJid fullJid, String sessionId) {
        this.fullJid = fullJid;
        this.sessionId = sessionId;
    }

    public FullJid getFullJid() {
        return fullJid;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public int hashCode() {
        int hashCode = 31 * fullJid.hashCode();
        hashCode = 31 * hashCode + sessionId.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FullJidAndSessionId)) {
            return false;
        }
        FullJidAndSessionId otherFullJidAndSessionId = (FullJidAndSessionId) other;
        return fullJid.equals(otherFullJidAndSessionId.fullJid)
                && sessionId.equals(otherFullJidAndSessionId.sessionId);
    }
}
