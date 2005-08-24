/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

package org.jivesoftware.smack;

/**
 * A listener that is fired any time a roster is changed or the presence of
 * a user in the roster is changed.
 *
 * @author Matt Tucker
 */
public interface RosterListener {

    /**
     * Called when a roster entry is added or removed.
     */
    public void rosterModified();

    /**
     * Called when the presence of a roster entry is changed.
     *
     * @param XMPPAddress the XMPP address of the user who's presence has changed,
     *      including the resource.
     */
    public void presenceChanged(String XMPPAddress);
}

