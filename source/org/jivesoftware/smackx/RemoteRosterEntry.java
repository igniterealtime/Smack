/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
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
    private List groupNames = new ArrayList();

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
			groupNames = new ArrayList(Arrays.asList(groups));
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
    public Iterator getGroupNames() {
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
            return (String[])
                (Collections
                    .unmodifiableList(groupNames)
                    .toArray(new String[groupNames.size()]));
        }
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<item jid=\"").append(user).append("\"");
        if (name != null) {
            buf.append(" name=\"").append(name).append("\"");
        }
        buf.append(">");
        synchronized (groupNames) {
            for (int i = 0; i < groupNames.size(); i++) {
                String groupName = (String) groupNames.get(i);
                buf.append("<group>").append(groupName).append("</group>");
            }
        }
        buf.append("</item>");
        return buf.toString();
    }

}
