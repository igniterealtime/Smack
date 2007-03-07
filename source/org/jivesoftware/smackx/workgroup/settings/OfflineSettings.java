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

package org.jivesoftware.smackx.workgroup.settings;

import org.jivesoftware.smackx.workgroup.util.ModelUtil;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class OfflineSettings extends IQ {
    private String redirectURL;

    private String offlineText;
    private String emailAddress;
    private String subject;

    public String getRedirectURL() {
        if (!ModelUtil.hasLength(redirectURL)) {
            return "";
        }
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public String getOfflineText() {
        if (!ModelUtil.hasLength(offlineText)) {
            return "";
        }
        return offlineText;
    }

    public void setOfflineText(String offlineText) {
        this.offlineText = offlineText;
    }

    public String getEmailAddress() {
        if (!ModelUtil.hasLength(emailAddress)) {
            return "";
        }
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSubject() {
        if (!ModelUtil.hasLength(subject)) {
            return "";
        }
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean redirects() {
        return (ModelUtil.hasLength(getRedirectURL()));
    }

    public boolean isConfigured(){
        return ModelUtil.hasLength(getEmailAddress()) &&
               ModelUtil.hasLength(getSubject()) &&
               ModelUtil.hasLength(getOfflineText());
    }

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "offline-settings";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=");
        buf.append('"');
        buf.append(NAMESPACE);
        buf.append('"');
        buf.append("></").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }


    /**
     * Packet extension provider for AgentStatusRequest packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            OfflineSettings offlineSettings = new OfflineSettings();

            boolean done = false;
            String redirectPage = null;
            String subject = null;
            String offlineText = null;
            String emailAddress = null;

            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("redirectPage".equals(parser.getName()))) {
                    redirectPage = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("subject".equals(parser.getName()))) {
                    subject = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("offlineText".equals(parser.getName()))) {
                    offlineText = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("emailAddress".equals(parser.getName()))) {
                    emailAddress = parser.nextText();
                }
                else if (eventType == XmlPullParser.END_TAG && "offline-settings".equals(parser.getName())) {
                    done = true;
                }
            }

            offlineSettings.setEmailAddress(emailAddress);
            offlineSettings.setRedirectURL(redirectPage);
            offlineSettings.setSubject(subject);
            offlineSettings.setOfflineText(offlineText);
            return offlineSettings;
        }
    }
}

