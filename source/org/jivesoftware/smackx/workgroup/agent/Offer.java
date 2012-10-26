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

package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A class embodying the semantic agent chat offer; specific instances allow the acceptance or
 * rejecting of the offer.<br>
 *
 * @author Matt Tucker
 * @author loki der quaeler
 * @author Derek DeMoro
 */
public class Offer {

    private Connection connection;
    private AgentSession session;

    private String sessionID;
    private String userJID;
    private String userID;
    private String workgroupName;
    private Date expiresDate;
    private Map<String, List<String>> metaData;
    private OfferContent content;

    private boolean accepted = false;
    private boolean rejected = false;

    /**
     * Creates a new offer.
     *
     * @param conn the XMPP connection with which the issuing session was created.
     * @param agentSession the agent session instance through which this offer was issued.
     * @param userID  the userID of the user from which the offer originates.
     * @param userJID the XMPP address of the user from which the offer originates.
     * @param workgroupName the fully qualified name of the workgroup.
     * @param expiresDate the date at which this offer expires.
     * @param sessionID the session id associated with the offer.
     * @param metaData the metadata associated with the offer.
     * @param content content of the offer. The content explains the reason for the offer
     *        (e.g. user request, transfer)
     */
    Offer(Connection conn, AgentSession agentSession, String userID,
            String userJID, String workgroupName, Date expiresDate,
            String sessionID, Map<String, List<String>> metaData, OfferContent content)
    {
        this.connection = conn;
        this.session = agentSession;
        this.userID = userID;
        this.userJID = userJID;
        this.workgroupName = workgroupName;
        this.expiresDate = expiresDate;
        this.sessionID = sessionID;
        this.metaData = metaData;
        this.content = content;
    }

    /**
     * Accepts the offer.
     */
    public void accept() {
        Packet acceptPacket = new AcceptPacket(this.session.getWorkgroupJID());
        connection.sendPacket(acceptPacket);
        // TODO: listen for a reply.
        accepted = true;
    }

    /**
     * Rejects the offer.
     */
    public void reject() {
        RejectPacket rejectPacket = new RejectPacket(this.session.getWorkgroupJID());
        connection.sendPacket(rejectPacket);
        // TODO: listen for a reply.
        rejected = true;
    }

    /**
     * Returns the userID that the offer originates from. In most cases, the
     * userID will simply be the JID of the requesting user. However, users can
     * also manually specify a userID for their request. In that case, that value will
     * be returned.
     *
     * @return the userID of the user from which the offer originates.
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the JID of the user that made the offer request.
     *
     * @return the user's JID.
     */
    public String getUserJID() {
        return userJID;
    }

    /**
     * The fully qualified name of the workgroup (eg support@example.com).
     *
     * @return the name of the workgroup.
     */
    public String getWorkgroupName() {
        return this.workgroupName;
    }

    /**
     * The date when the offer will expire. The agent must {@link #accept()}
     * the offer before the expiration date or the offer will lapse and be
     * routed to another agent. Alternatively, the agent can {@link #reject()}
     * the offer at any time if they don't wish to accept it..
     *
     * @return the date at which this offer expires.
     */
    public Date getExpiresDate() {
        return this.expiresDate;
    }

    /**
     * The session ID associated with the offer.
     *
     * @return the session id associated with the offer.
     */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * The meta-data associated with the offer.
     *
     * @return the offer meta-data.
     */
    public Map<String, List<String>> getMetaData() {
        return this.metaData;
    }

    /**
     * Returns the content of the offer. The content explains the reason for the offer
     * (e.g. user request, transfer)
     *
     * @return the content of the offer.
     */
    public OfferContent getContent() {
        return content;
    }

    /**
     * Returns true if the agent accepted this offer.
     *
     * @return true if the agent accepted this offer.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Return true if the agent rejected this offer.
     *
     * @return true if the agent rejected this offer.
     */
    public boolean isRejected() {
        return rejected;
    }

    /**
     * Packet for rejecting offers.
     */
    private class RejectPacket extends IQ {

        RejectPacket(String workgroup) {
            this.setTo(workgroup);
            this.setType(IQ.Type.SET);
        }

        public String getChildElementXML() {
            return "<offer-reject id=\"" + Offer.this.getSessionID() +
                    "\" xmlns=\"http://jabber.org/protocol/workgroup" + "\"/>";
        }
    }

    /**
     * Packet for accepting an offer.
     */
    private class AcceptPacket extends IQ {

        AcceptPacket(String workgroup) {
            this.setTo(workgroup);
            this.setType(IQ.Type.SET);
        }

        public String getChildElementXML() {
            return "<offer-accept id=\"" + Offer.this.getSessionID() +
                    "\" xmlns=\"http://jabber.org/protocol/workgroup" + "\"/>";
        }
    }

}