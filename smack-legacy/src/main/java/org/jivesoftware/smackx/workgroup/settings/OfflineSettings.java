/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.settings;

import java.io.IOException;

import org.jivesoftware.smack.packet.SimpleIQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class OfflineSettings extends SimpleIQ {
    private String redirectURL;

    private String offlineText;
    private String emailAddress;
    private String subject;

    public String getRedirectURL() {
        if (!StringUtils.isNotEmpty(redirectURL)) {
            return "";
        }
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public String getOfflineText() {
        if (!StringUtils.isNotEmpty(offlineText)) {
            return "";
        }
        return offlineText;
    }

    public void setOfflineText(String offlineText) {
        this.offlineText = offlineText;
    }

    public String getEmailAddress() {
        if (!StringUtils.isNotEmpty(emailAddress)) {
            return "";
        }
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSubject() {
        if (!StringUtils.isNotEmpty(subject)) {
            return "";
        }
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean redirects() {
        return (StringUtils.isNotEmpty(getRedirectURL()));
    }

    public boolean isConfigured() {
        return StringUtils.isNotEmpty(getEmailAddress()) &&
               StringUtils.isNotEmpty(getSubject()) &&
               StringUtils.isNotEmpty(getOfflineText());
    }

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "offline-settings";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public OfflineSettings() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Stanza extension provider for AgentStatusRequest packets.
     */
    public static class InternalProvider extends IQProvider<OfflineSettings> {

        @Override
        public OfflineSettings parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
            OfflineSettings offlineSettings = new OfflineSettings();

            boolean done = false;
            String redirectPage = null;
            String subject = null;
            String offlineText = null;
            String emailAddress = null;

            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && "redirectPage".equals(parser.getName())) {
                    redirectPage = parser.nextText();
                }
                else if (eventType == XmlPullParser.START_TAG && "subject".equals(parser.getName())) {
                    subject = parser.nextText();
                }
                else if (eventType == XmlPullParser.START_TAG && "offlineText".equals(parser.getName())) {
                    offlineText = parser.nextText();
                }
                else if (eventType == XmlPullParser.START_TAG && "emailAddress".equals(parser.getName())) {
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

