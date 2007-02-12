/**
 * $RCSfile$
 * $Revision: 2407 $
 * $Date: 2004-11-02 15:37:00 -0800 (Tue, 02 Nov 2004) $
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

package org.jivesoftware.smackx;

/**
 * Represents the current state of a users interaction with another user. Implemented according to
 * <a href="http://www.xmpp.org/extensions/xep-0085.html">XEP-0085</a>.
 *
 * @author Alexander Wenckus
 */
public enum ChatState {
    /**
     * User is actively participating in the chat session.
     */
    active,
    /**
     * User is composing a message.
     */
    composing,
    /**
     * User had been composing but now has stopped.
     */
    paused,
    /**
     * User has not been actively participating in the chat session.
     */
    inactive,
    /**
     * User has effectively ended their participation in the chat session.
     */
    gone
}
