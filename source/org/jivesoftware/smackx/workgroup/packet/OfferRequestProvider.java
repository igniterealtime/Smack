/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
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
