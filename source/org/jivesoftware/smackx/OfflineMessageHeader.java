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

import org.jivesoftware.smackx.packet.DiscoverItems;

/**
 * The OfflineMessageHeader holds header information of an offline message. The header
 * information was retrieved using the {@link OfflineMessageManager} class.<p>
 *
 * Each offline message is identified by the target user of the offline message and a unique stamp.
 * Use {@link OfflineMessageManager#getMessages(java.util.List)} to retrieve the whole message.
 *
 * @author Gaston Dombiak
 */
public class OfflineMessageHeader {
    /**
     * Bare JID of the user that was offline when the message was sent.
     */
    private String user;
    /**
     * Full JID of the user that sent the message.
     */
    private String jid;
    /**
     * Stamp that uniquely identifies the offline message. This stamp will be used for
     * getting the specific message or delete it. The stamp may be of the form UTC timestamps
     * but it is not required to have that format.
     */
    private String stamp;

    public OfflineMessageHeader(DiscoverItems.Item item) {
        super();
        user = item.getEntityID();
        jid = item.getName();
        stamp = item.getNode();
    }

    /**
     * Returns the bare JID of the user that was offline when the message was sent.
     *
     * @return the bare JID of the user that was offline when the message was sent.
     */
    public String getUser() {
        return user;
    }

    /**
     * Returns the full JID of the user that sent the message.
     *
     * @return the full JID of the user that sent the message.
     */
    public String getJid() {
        return jid;
    }

    /**
     * Returns the stamp that uniquely identifies the offline message. This stamp will
     * be used for getting the specific message or delete it. The stamp may be of the
     * form UTC timestamps but it is not required to have that format.
     *
     * @return the stamp that uniquely identifies the offline message.
     */
    public String getStamp() {
        return stamp;
    }
}
