/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
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