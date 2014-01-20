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
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Packet used for requesting information about occupants of a room or for retrieving information
 * such information.
 *
 * @author Gaston Dombiak
 */
public class OccupantsInfo extends IQ {

    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "occupants-info";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private String roomID;
    private final Set<OccupantInfo> occupants;

    public OccupantsInfo(String roomID) {
        this.roomID = roomID;
        this.occupants = new HashSet<OccupantInfo>();
    }

    public String getRoomID() {
        return roomID;
    }

    public int getOccupantsCount() {
        return occupants.size();
    }

    public Set<OccupantInfo> getOccupants() {
        return Collections.unmodifiableSet(occupants);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE);
        buf.append("\" roomID=\"").append(roomID).append("\">");
        synchronized (occupants) {
            for (OccupantInfo occupant : occupants) {
                buf.append("<occupant>");
                // Add the occupant jid
                buf.append("<jid>");
                buf.append(occupant.getJID());
                buf.append("</jid>");
                // Add the occupant nickname
                buf.append("<name>");
                buf.append(occupant.getNickname());
                buf.append("</name>");
                // Add the date when the occupant joined the room
                buf.append("<joined>");
                buf.append(UTC_FORMAT.format(occupant.getJoined()));
                buf.append("</joined>");
                buf.append("</occupant>");
            }
        }
        buf.append("</").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }

    public static class OccupantInfo {

        private String jid;
        private String nickname;
        private Date joined;

        public OccupantInfo(String jid, String nickname, Date joined) {
            this.jid = jid;
            this.nickname = nickname;
            this.joined = joined;
        }

        public String getJID() {
            return jid;
        }

        public String getNickname() {
            return nickname;
        }

        public Date getJoined() {
            return joined;
        }
    }

    /**
     * Packet extension provider for AgentStatusRequest packets.
     */
    public static class Provider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }
            OccupantsInfo occupantsInfo = new OccupantsInfo(parser.getAttributeValue("", "roomID"));

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) &&
                        ("occupant".equals(parser.getName()))) {
                    occupantsInfo.occupants.add(parseOccupantInfo(parser));
                } else if (eventType == XmlPullParser.END_TAG &&
                        ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
            return occupantsInfo;
        }

        private OccupantInfo parseOccupantInfo(XmlPullParser parser) throws Exception {

            boolean done = false;
            String jid = null;
            String nickname = null;
            Date joined = null;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("jid".equals(parser.getName()))) {
                    jid = parser.nextText();
                } else if ((eventType == XmlPullParser.START_TAG) &&
                        ("nickname".equals(parser.getName()))) {
                    nickname = parser.nextText();
                } else if ((eventType == XmlPullParser.START_TAG) &&
                        ("joined".equals(parser.getName()))) {
                    joined = UTC_FORMAT.parse(parser.nextText());
                } else if (eventType == XmlPullParser.END_TAG &&
                        "occupant".equals(parser.getName())) {
                    done = true;
                }
            }
            return new OccupantInfo(jid, nickname, joined);
        }
    }
}
