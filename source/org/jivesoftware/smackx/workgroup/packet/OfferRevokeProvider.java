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
