/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.iqlast.packet;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A last activity IQ for retrieving information about the last activity associated with a Jabber ID.
 * LastActivity (XEP-0012) allows for retrieval of how long a particular user has been idle and the
 * message the specified when doing so. Use {@link org.jivesoftware.smackx.iqlast.LastActivityManager}
 * to get the last activity of a user.
 *
 * @author Derek DeMoro
 * @author Florian Schmaus
 */
public class LastActivity extends IQ {

    public static final String NAMESPACE = "jabber:iq:last";

    public long lastActivity = -1;
    public String message;

    public LastActivity() {
        setType(IQ.Type.GET);
    }

    public LastActivity(String to) {
        this();
        setTo(to);
    }

    @Override
    public XmlStringBuilder getChildElementXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement("query");
        xml.xmlnsAttribute(NAMESPACE);
        if (lastActivity != -1) {
            xml.attribute("seconds", Long.toString(lastActivity));
        }
        // We don't support adding the optional message attribute, because it is usually only added
        // by XMPP servers and not by client entities.
        xml.closeEmptyElement();
        return xml;
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

        public IQ parseIQ(XmlPullParser parser) throws SmackException, XmlPullParserException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new SmackException("Parser not in proper position, or bad XML.");
            }

            LastActivity lastActivity = new LastActivity();
            String seconds = parser.getAttributeValue("", "seconds");
            if (seconds != null) {
                try {
                    lastActivity.setLastActivity(Long.parseLong(seconds));
                } catch (NumberFormatException e) {
                    throw new SmackException("Could not parse last activity number", e);
                }
            }
            try {
                lastActivity.setMessage(parser.nextText());
            } catch (IOException e) {
                throw new SmackException(e);
            }
            return lastActivity;
        }
    }
}
