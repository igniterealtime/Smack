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

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;

/**
 * A listener that is fired any time a roster is changed or the presence of
 * a user in the roster is changed.
 * 
 * @see Roster#addRosterListener(RosterListener)
 * @author Matt Tucker
 */
public interface RosterListener {

    /**
     * Called when roster entries are added.
     *
     * @param addresses the XMPP addresses of the contacts that have been added to the roster.
     */
    public void entriesAdded(Collection<String> addresses);

    /**
     * Called when a roster entries are updated.
     *
     * @param addresses the XMPP addresses of the contacts whose entries have been updated.
     */
    public void entriesUpdated(Collection<String> addresses);

    /**
     * Called when a roster entries are removed.
     *
     * @param addresses the XMPP addresses of the contacts that have been removed from the roster.
     */
    public void entriesDeleted(Collection<String> addresses);

    /**
     * Called when the presence of a roster entry is changed. Care should be taken
     * when using the presence data delivered as part of this event. Specifically,
     * when a user account is online with multiple resources, the UI should account
     * for that. For example, say a user is online with their desktop computer and
     * mobile phone. If the user logs out of the IM client on their mobile phone, the
     * user should not be shown in the roster (contact list) as offline since they're
     * still available as another resource.<p>
     *
     * To get the current "best presence" for a user after the presence update, query the roster:
     * <pre>
     *    String user = presence.getFrom();
     *    Presence bestPresence = roster.getPresence(user);
     * </pre>
     *
     * That will return the presence value for the user with the highest priority and
     * availability.
     *
     * Note that this listener is triggered for presence (mode) changes only
     * (e.g presence of types available and unavailable. Subscription-related
     * presence packets will not cause this method to be called.
     *
     * @param presence the presence that changed.
     * @see Roster#getPresence(String)
     */
    public void presenceChanged(Presence presence);
}