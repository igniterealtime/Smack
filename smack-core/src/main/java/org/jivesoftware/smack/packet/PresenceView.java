/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.packet;

public interface PresenceView extends StanzaView {

    /**
     * Returns the type of this presence stanza.
     *
     * @return the type of the presence stanza.
     */
    Presence.Type getType();

    /**
     * Returns the status message of the presence update, or <code>null</code> if there
     * is not a status. The status is free-form text describing a user's presence
     * (i.e., "gone to lunch").
     *
     * @return the status message.
     */
    String getStatus();

    /**
     * Returns the priority of the presence.
     *
     * @return the priority.
     * @see <a href="https://tools.ietf.org/html/rfc6121#section-4.7.2.3">RFC 6121 § 4.7.2.3. Priority Element</a>
     */
    int getPriority();

    /**
     * Returns the priority of the presence.
     *
     * @return the priority.
     * @see <a href="https://tools.ietf.org/html/rfc6121#section-4.7.2.3">RFC 6121 § 4.7.2.3. Priority Element</a>
     */
    byte getPriorityByte();

    /**
     * Returns the mode of the presence update.
     *
     * @return the mode.
     */
    Presence.Mode getMode();
}
