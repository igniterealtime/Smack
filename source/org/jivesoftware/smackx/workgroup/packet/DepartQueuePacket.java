/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.*;

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
        StringBuffer buf = new StringBuffer("<depart-queue xmlns=\"xmpp:workgroup\"");

        if (this.user != null) {
            buf.append("><jid>").append(this.user).append("</jid></depart-queue>");
        }
        else {
            buf.append("/>");
        }

        return buf.toString();
    }
}