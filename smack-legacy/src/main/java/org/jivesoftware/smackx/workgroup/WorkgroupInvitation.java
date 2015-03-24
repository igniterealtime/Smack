/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup;

import java.util.List;
import java.util.Map;

import org.jxmpp.jid.Jid;

/**
 * An immutable class wrapping up the basic information which comprises a group chat invitation.
 *
 * @author loki der quaeler
 */
public class WorkgroupInvitation {

    protected Jid uniqueID;

    protected String sessionID;

    protected Jid groupChatName;
    protected Jid issuingWorkgroupName;
    protected String messageBody;
    protected Jid invitationSender;
    protected Map<String, List<String>> metaData;

    /**
     * This calls the 5-argument constructor with a null MetaData argument value
     *
     * @param jid the jid string with which the issuing AgentSession or Workgroup instance
     *                  was created
     * @param group the jid of the room to which the person is invited
     * @param workgroup the jid of the workgroup issuing the invitation
     * @param sessID the session id associated with the pending chat
     * @param msgBody the body of the message which contained the invitation
     * @param from the user jid who issued the invitation, if known, null otherwise
     */
    public WorkgroupInvitation (Jid jid, Jid group, Jid workgroup,
                       String sessID, String msgBody, Jid from) {
        this(jid, group, workgroup, sessID, msgBody, from, null);
    }

    /**
     * @param jid the jid string with which the issuing AgentSession or Workgroup instance
     *                  was created
     * @param group the jid of the room to which the person is invited
     * @param workgroup the jid of the workgroup issuing the invitation
     * @param sessID the session id associated with the pending chat
     * @param msgBody the body of the message which contained the invitation
     * @param from the user jid who issued the invitation, if known, null otherwise
     * @param metaData the metadata sent with the invitation
     */
    public WorkgroupInvitation (Jid jid, Jid group, Jid workgroup, String sessID, String msgBody,
                       Jid from, Map<String, List<String>> metaData) {
        super();

        this.uniqueID = jid;
        this.sessionID = sessID;
        this.groupChatName = group;
        this.issuingWorkgroupName = workgroup;
        this.messageBody = msgBody;
        this.invitationSender = from;
        this.metaData = metaData;
    }

    /**
     * @return the jid string with which the issuing AgentSession or Workgroup instance
     *  was created.
     */
    public Jid getUniqueID () {
        return this.uniqueID;
    }

    /**
     * @return the session id associated with the pending chat; working backwards temporally,
     *              this session id should match the session id to the corresponding offer request
     *              which resulted in this invitation.
     */
    public String getSessionID () {
        return this.sessionID;
    }

    /**
     * @return the jid of the room to which the person is invited.
     */
    public Jid getGroupChatName () {
        return this.groupChatName;
    }

    /**
     * @return the name of the workgroup from which the invitation was issued.
     */
    public Jid getWorkgroupName () {
        return this.issuingWorkgroupName;
    }

    /**
     * @return the contents of the body-block of the message that housed this invitation.
     */
    public String getMessageBody () {
        return this.messageBody;
    }

    /**
     * @return the user who issued the invitation, or null if it wasn't known.
     */
    public Jid getInvitationSender () {
        return this.invitationSender;
    }

    /**
     * @return the meta data associated with the invitation, or null if this instance was
     *              constructed with none
     */
    public Map<String, List<String>> getMetaData () {
        return this.metaData;
    }

}
