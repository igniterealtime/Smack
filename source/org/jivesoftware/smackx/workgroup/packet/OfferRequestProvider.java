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

import java.util.*;

import org.jivesoftware.smackx.workgroup.*;
import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

import org.xmlpull.v1.XmlPullParser;

/**
 * An IQProvider for agent offer requests.
 *
 * @author loki der quaeler
 */
public class OfferRequestProvider implements IQProvider {

    public OfferRequestProvider () {
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        int eventType = parser.getEventType();
        String uid = null;
        String sessionID = null;
        int timeout = -1;
        boolean done = false;
        Map metaData = new HashMap();

        if (eventType != XmlPullParser.START_TAG) {
            // throw exception
        }

        uid = parser.getAttributeValue("", "jid");
        if (uid == null) {
            // throw exception
        }

        parser.nextTag();
        while (!done) {
            eventType = parser.getEventType();

            if (eventType == XmlPullParser.START_TAG) {
                String elemName = parser.getName();

                if ("timeout".equals(elemName)) {
                    timeout = Integer.parseInt(parser.nextText());
                }
                else if (MetaData.ELEMENT_NAME.equals(elemName)) {
                    metaData = MetaDataUtils.parseMetaData(parser);
                }
                else
                   if (SessionID.ELEMENT_NAME.equals(elemName)) {
                       sessionID = parser.getAttributeValue("", "session");

                       parser.nextTag();
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if ("offer".equals(parser.getName())) {
                    done = true;
                }
                else {
                    parser.nextTag();
                }
            }
            else {
                parser.nextTag();
            }
        }

        OfferRequestPacket offerRequest = new OfferRequestPacket(uid, timeout, metaData, sessionID);
        offerRequest.setType(IQ.Type.SET);

        return offerRequest;
    }

    public static class OfferRequestPacket extends IQ {

        private int timeout;
        private String userID;
        private Map metaData;
        private String sessionID;

        public OfferRequestPacket(String uid, int timeout, Map metaData, String sID) {
            this.userID = uid;
            this.timeout = timeout;
            this.metaData = metaData;
            this.sessionID = sID;
        }

        public String getUserID() {
            return userID;
        }

        /**
         * Returns the session id which will be associated with the customer for whom this offer
         *  is extended, or null if the offer did not contain one.
         *
         * @return the session id associated to the customer
         */
        public String getSessionID() {
            return sessionID;
        }

        /**
         * Returns the number of seconds the agent has to accept the offer before
         * it times out.
         *
         * @return the offer timeout (in seconds).
         */
        public int getTimeout() {
            return this.timeout;
        }

        public Map getMetaData() {
            return this.metaData;
        }

        public String getChildElementXML () {
            StringBuffer buf = new StringBuffer();

            buf.append("<offer xmlns=\"xmpp:workgroup\" jid=\"").append(userID).append("\">");
            buf.append("<timeout>").append(timeout).append("</timeout>");

            if (sessionID != null) {
                buf.append('<').append(SessionID.ELEMENT_NAME);
                buf.append(" session=\"");
                buf.append(getSessionID()).append("\" xmlns=\"");
                buf.append(SessionID.NAMESPACE).append("\"/>");
            }

            if (metaData != null) {
                buf.append(MetaDataUtils.encodeMetaData(metaData));
            }

            buf.append("</offer>");

            return buf.toString();
        }
    }
}
