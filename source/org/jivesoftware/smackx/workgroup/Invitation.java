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

package org.jivesoftware.smackx.workgroup;

import java.util.Map;

/**
 * An immutable class wrapping up the basic information which comprises a group chat invitation.
 *
 * @author loki der quaeler
 */
public class Invitation {

    protected String uniqueID;

    protected String sessionID;

    protected String groupChatName;
    protected String issuingWorkgroupName;
    protected String messageBody;
    protected String invitationSender;
    protected Map metaData;

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
    public Invitation (String jid, String group, String workgroup,
                       String sessID, String msgBody, String from) {
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
    public Invitation (String jid, String group, String workgroup, String sessID, String msgBody,
                       String from, Map metaData) {
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
    public String getUniqueID () {
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
    public String getGroupChatName () {
        return this.groupChatName;
    }

    /**
     * @return the name of the workgroup from which the invitation was issued.
     */
    public String getWorkgroupName () {
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
    public String getInvitationSender () {
        return this.invitationSender;
    }

    /**
     * @return the meta data associated with the invitation, or null if this instance was
     *              constructed with none
     */
    public Map getMetaData () {
        return this.metaData;
    }

}
