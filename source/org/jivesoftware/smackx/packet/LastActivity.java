/**
 * $RCSfile$
 * $Revision: 2407 $
 * $Date: 2004-11-02 15:37:00 -0800 (Tue, 02 Nov 2004) $
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

/**
 * A last activity IQ for retrieving information about the last activity associated with a Jabber ID.
 * LastActivity (JEP-012) allows for retrieval of how long a particular user has been idle and the
 * message the specified when doing so. Use {@link org.jivesoftware.smackx.LastActivityManager}
 * to get the last activity of a user.
 *
 * @author Derek DeMoro
 */
public class LastActivity extends IQ {

    public long lastActivity = -1;
    public String message;

    public LastActivity() {
        setType(IQ.Type.GET);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
		buf.append("<query xmlns=\"jabber:iq:last\"");
		if (lastActivity != -1) {
			buf.append(" seconds=\"").append(lastActivity).append("\"");

		}
		buf.append("></query>");
        return buf.toString();
    }


    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }


    private void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns number of seconds that have passed since the user last logged out.
     * If the user is offline, 0 will be returned.
     *
     * @return the number of seconds that have passed since the user last logged out.
     */
    public long getIdleTime() {
        return lastActivity;
    }


    /**
     * Returns the status message of the last unavailable presence received from the user.
     *
     * @return the status message of the last unavailable presence received from the user
     */
    public String getStatusMessage() {
        return message;
    }


    /**
     * The IQ Provider for LastActivity.
     *
     * @author Derek DeMoro
     */
    public static class Provider implements IQProvider {

        public Provider() {
            super();
        }

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            LastActivity lastActivity = new LastActivity();
            try {
                String seconds = parser.getAttributeValue("", "seconds");
                String message = parser.nextText();
                if (seconds != null) {
                    long xmlSeconds = new Double(seconds).longValue();
                    lastActivity.setLastActivity((int)xmlSeconds);
                }

                if (message != null) {
                    lastActivity.setMessage(message);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return lastActivity;
        }
    }

    /**
     * Retrieve the last activity of a particular jid.
     * @param con the current XMPPConnection.
     * @param jid the JID of the user.
     * @return the LastActivity packet of the jid.
     * @throws XMPPException thrown if a server error has occured.
     * @deprecated This method only retreives the lapsed time since the last logout of a particular jid. 
     * Replaced by {@link  org.jivesoftware.smackx.LastActivityManager#getLastActivity(XMPPConnection, String)  getLastActivity}
     */
    public static LastActivity getLastActivity(XMPPConnection con, String jid) throws XMPPException {
        LastActivity activity = new LastActivity();
        jid = StringUtils.parseBareAddress(jid);
        activity.setTo(jid);

        PacketCollector collector = con.createPacketCollector(new PacketIDFilter(activity.getPacketID()));
        con.sendPacket(activity);

        LastActivity response = (LastActivity) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }
}
