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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;


public class OfferConfirmation extends IQ {
    private String userJID;
    private long sessionID;

    public String getUserJID() {
        return userJID;
    }

    public void setUserJID(String userJID) {
        this.userJID = userJID;
    }

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }


    public void notifyService(XMPPConnection con, String workgroup, String createdRoomName) {
        NotifyServicePacket packet = new NotifyServicePacket(workgroup, createdRoomName);
        con.sendPacket(packet);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<offer-confirmation xmlns=\"http://jabber.org/protocol/workgroup\">");
        buf.append("</offer-confirmation>");
        return buf.toString();
    }

    public static class Provider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            final OfferConfirmation confirmation = new OfferConfirmation();

            boolean done = false;
            while (!done) {
                parser.next();
                String elementName = parser.getName();
                if (parser.getEventType() == XmlPullParser.START_TAG && "user-jid".equals(elementName)) {
                    try {
                        confirmation.setUserJID(parser.nextText());
                    }
                    catch (NumberFormatException nfe) {
                    }
                }
                else if (parser.getEventType() == XmlPullParser.START_TAG && "session-id".equals(elementName)) {
                    try {
                        confirmation.setSessionID(Long.valueOf(parser.nextText()));
                    }
                    catch (NumberFormatException nfe) {
                    }
                }
                else if (parser.getEventType() == XmlPullParser.END_TAG && "offer-confirmation".equals(elementName)) {
                    done = true;
                }
            }


            return confirmation;
        }
    }


    /**
     * Packet for notifying server of RoomName
     */
    private class NotifyServicePacket extends IQ {
        String roomName;

        NotifyServicePacket(String workgroup, String roomName) {
            this.setTo(workgroup);
            this.setType(IQ.Type.RESULT);

            this.roomName = roomName;
        }

        public String getChildElementXML() {
            return "<offer-confirmation  roomname=\"" + roomName + "\" xmlns=\"http://jabber.org/protocol/workgroup" + "\"/>";
        }
    }


}
