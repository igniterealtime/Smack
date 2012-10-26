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

package org.jivesoftware.smackx;

import java.util.*;

/**
 * Represents a roster item, which consists of a JID and , their name and
 * the groups the roster item belongs to. This roster item does not belong
 * to the local roster. Therefore, it does not persist in the server.<p>
 *
 * The idea of a RemoteRosterEntry is to be used as part of a roster exchange.
 *
 * @author Gaston Dombiak
 */
public class RemoteRosterEntry {

    private String user;
    private String name;
    private final List<String> groupNames = new ArrayList<String>();

    /**
     * Creates a new remote roster entry.
     *
     * @param user the user.
     * @param name the user's name.
     * @param groups the list of group names the entry will belong to, or <tt>null</tt> if the
     *      the roster entry won't belong to a group.
     */
    public RemoteRosterEntry(String user, String name, String [] groups) {
        this.user = user;
        this.name = name;
		if (groups != null) {
            groupNames.addAll(Arrays.asList(groups));
		}
    }

    /**
     * Returns the user.
     *
     * @return the user.
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the user's name.
     *
     * @return the user's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an Iterator for the group names (as Strings) that the roster entry
     * belongs to.
     *
     * @return an Iterator for the group names.
     */
    public Iterator<String> getGroupNames() {
        synchronized (groupNames) {
            return Collections.unmodifiableList(groupNames).iterator();
        }
    }

    /**
     * Returns a String array for the group names that the roster entry
     * belongs to.
     *
     * @return a String[] for the group names.
     */
    public String[] getGroupArrayNames() {
        synchronized (groupNames) {
            return Collections.unmodifiableList(groupNames).toArray(new String[groupNames.size()]);
        }
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<item jid=\"").append(user).append("\"");
        if (name != null) {
            buf.append(" name=\"").append(name).append("\"");
        }
        buf.append(">");
        synchronized (groupNames) {
            for (String groupName : groupNames) {
                buf.append("<group>").append(groupName).append("</group>");
            }
        }
        buf.append("</item>");
        return buf.toString();
    }

}
