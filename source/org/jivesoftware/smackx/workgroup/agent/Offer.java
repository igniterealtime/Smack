/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

import java.util.Date;
import java.util.Map;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

/**
 * A class embodying the semantic agent chat offer; specific instances allow
 * the acceptance or rejecting of the offer.<br>
 * 
 * @author Matt Tucker
 * @author Derek DeMoro
 */
public class Offer {

    private XMPPConnection connection;
    private WorkgroupSession session;

    private String sessionID;
    private String userID;
    private String workgroupName;
    private Date expiresDate;
    private Map metaData;

   /**
    * Creates a new offer.
    *
    * @param conn          the XMPP connection with which the issuing session was created.
    * @param workgroupSession  the agent session instance through which this offer was issued.
    * @param userID        the XMPP address of the user from which the offer originates.
    * @param workgroupName the fully qualified name of the workgroup.
    * @param expiresDate   the date at which this offer expires.
    * @param sessionID     the session id associated with the offer.
    * @param metaData      the metadata associated with the offer.
    */
    public Offer(XMPPConnection conn, WorkgroupSession workgroupSession, String userID,
                String workgroupName, Date expiresDate,
                String sessionID, Map metaData)
    {
        this.connection = conn;
        this.session = workgroupSession;
        this.userID = userID;
        this.workgroupName = workgroupName;
        this.expiresDate = expiresDate;
        this.sessionID = sessionID;
        this.metaData = metaData;
    }

   /**
    * Accepts the offer.
    */
    public void accept() {
        Packet acceptPacket = new AcceptPacket( this.session.getWorkgroupName() );
        connection.sendPacket( acceptPacket );
        // TODO: listen for a reply.
    }

   /**
    * Rejects the offer.
    */
    public void reject() {
        RejectPacket rejectPacket = new RejectPacket( this.session.getWorkgroupName() );
        connection.sendPacket( rejectPacket );
        // TODO: listen for a reply.
    }

   /**
    * Returns the XMPP address of the user from which the offer originates
    * (eg jsmith@example.com/WebClient). For example, if the user jsmith initiates
    * a support request by joining the workgroup queue, then this user ID will be
    * jsmith's address.
    *
    * @return the XMPP address of the user from which the offer originates.
    */
    public String getUserID() {
        return userID;
    }

   /**
    * The fully qualified name of the workgroup (eg support@example.com)
    * that this offer is from.
    *
    * @return the name of the workgroup this offer is from.
    */
    public String getWorkgroupName() {
        return this.workgroupName;
    }

   /**
    * The date when this offer will expire. The agent must {@link #accept()}
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
    * The session ID associated with this offer.
    *
    * @return the session ID associated with this offer.
    */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * Returns the meta-data associated with this offer.
     *
     * @return the offer meta-data.
    */
    public Map getMetaData() {
        return this.metaData;
    }

   /**
    * Packet for rejecting offers.
    */
    private class RejectPacket extends IQ {

        RejectPacket( String workgroup ) {
        this.setTo( workgroup );
        this.setType( IQ.Type.SET );
    }

        public String getChildElementXML() {
            return "<offer-reject jid=\"" + Offer.this.getUserID() +
                    "\" xmlns=\"xmpp:workgroup" + "\"/>";
        }
    }

    /**
    * Packet for accepting an offer.
    */
    private class AcceptPacket extends IQ {

        AcceptPacket( String workgroup ) {
            this.setTo( workgroup );
            this.setType( IQ.Type.SET );
        }

        public String getChildElementXML() {
            return "<offer-accept jid=\"" + Offer.this.getUserID() +
                    "\" xmlns=\"xmpp:workgroup" + "\"/>";
        }
    }
}