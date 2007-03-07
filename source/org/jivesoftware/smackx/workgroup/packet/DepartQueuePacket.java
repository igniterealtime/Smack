/**
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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * A IQ packet used to depart a workgroup queue. There are two cases for issuing a depart
 * queue request:<ul>
 *     <li>The user wants to leave the queue. In this case, an instance of this class
 *         should be created without passing in a user address.
 *     <li>An administrator or the server removes wants to remove a user from the queue.
 *         In that case, the address of the user to remove from the queue should be
 *         used to create an instance of this class.</ul>
 *
 * @author loki der quaeler
 */
public class DepartQueuePacket extends IQ {

    private String user;

    /**
     * Creates a depart queue request packet to the specified workgroup.
     *
     * @param workgroup the workgroup to depart.
     */
    public DepartQueuePacket(String workgroup) {
        this(workgroup, null);
    }

    /**
     * Creates a depart queue request to the specified workgroup and for the
     * specified user.
     *
     * @param workgroup the workgroup to depart.
     * @param user the user to make depart from the queue.
     */
    public DepartQueuePacket(String workgroup, String user) {
        this.user = user;

        setTo(workgroup);
        setType(IQ.Type.SET);
        setFrom(user);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder("<depart-queue xmlns=\"http://jabber.org/protocol/workgroup\"");

        if (this.user != null) {
            buf.append("><jid>").append(this.user).append("</jid></depart-queue>");
        }
        else {
            buf.append("/>");
        }

        return buf.toString();
    }
}