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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;

import org.xmlpull.v1.XmlPullParser;

/**
 * An IQProvider class which has savvy about the offer-revoke tag.<br>
 *
 * @author loki der quaeler
 */
public class OfferRevokeProvider implements IQProvider {

    public IQ parseIQ (XmlPullParser parser) throws Exception {
        // The parser will be positioned on the opening IQ tag, so get the JID attribute.
        String uid = parser.getAttributeValue("", "jid");
        String reason = null;
        String sessionID = null;
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if ((eventType == XmlPullParser.START_TAG) && parser.getName().equals("reason")) {
                reason = parser.nextText();
            }
            else if ((eventType == XmlPullParser.START_TAG)
                         && parser.getName().equals(SessionID.ELEMENT_NAME)) {
                sessionID = parser.getAttributeValue("", "session");
            }
            else if ((eventType == XmlPullParser.END_TAG)
                                                      && parser.getName().equals("offer-revoke")) {
                done = true;
            }
        }

        return new OfferRevokePacket(uid, reason, sessionID);
    }

    public class OfferRevokePacket extends IQ {

        protected String userID;
        protected String sessionID;
        protected String reason;

        public OfferRevokePacket (String uid, String cause, String sid) {
            this.userID = uid;
            this.reason = cause;
            this.sessionID = sid;
        }

        public String getUserID () {
            return this.userID;
        }

        public String getReason () {
            return this.reason;
        }

        public String getSessionID () {
            return this.sessionID;
        }

        public String getChildElementXML () {
            StringBuffer buf = new StringBuffer();
            buf.append("<offer-revoke xmlns=\"xmpp:workgroup\" jid=\"").append(userID).append("\">");
            if (reason != null) {
                buf.append("<reason>").append(reason).append("</reason>");
            }
            buf.append("</offer-revoke>");
            return buf.toString();
        }
    }
}
