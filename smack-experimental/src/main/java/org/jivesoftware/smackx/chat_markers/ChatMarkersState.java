/**
 *
 * Copyright © 2018 Miguel Hincapie
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
package org.jivesoftware.smackx.chat_markers;

/**
 * Chat Markers elements (XEP-0333).
 *
 * @author Miguel Hincapie
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 * Markers</a>
 */
public enum ChatMarkersState {
    /**
     *  Indicates that a message can be marked with a Chat Marker and is therefore
     *  a "markable message".
     */
    markable,
    /**
     * The message has been received by a client.
     */
    received,
    /**
     * The message has been displayed to a user in a active chat and not in a system notification.
     */
    displayed,
    /**
     * The message has been acknowledged by some user interaction e.g. pressing an
     * acknowledgement button.
     */
    acknowledged
}
