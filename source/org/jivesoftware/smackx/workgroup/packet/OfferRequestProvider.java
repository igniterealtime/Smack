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
                buf.append(MetaDataUtils.serializeMetaData(metaData));
            }

            buf.append("</offer>");

            return buf.toString();
        }
    }
}
