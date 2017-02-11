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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class WorkgroupProperties extends IQ {

    private boolean authRequired;
    private String email;
    private String fullName;
    private String jid;

    public boolean isAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }


    /**
     * Element name of the stanza(/packet) extension.
     */
    public static final String ELEMENT_NAME = "workgroup-properties";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public WorkgroupProperties() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        if (StringUtils.isNotEmpty(getJid())) {
            buf.append("jid=\"" + getJid() + "\" ");
        }
        buf.setEmptyElement();
        return buf;
    }

    /**
     * Stanza(/Packet) extension provider for SoundSetting Packets.
     */
    public static class InternalProvider extends IQProvider<WorkgroupProperties> {

        @Override
        public WorkgroupProperties parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            WorkgroupProperties props = new WorkgroupProperties();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("authRequired".equals(parser.getName()))) {
                    props.setAuthRequired(Boolean.valueOf(parser.nextText()).booleanValue());
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("email".equals(parser.getName()))) {
                    props.setEmail(parser.nextText());
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("name".equals(parser.getName()))) {
                    props.setFullName(parser.nextText());
                }
                else if (eventType == XmlPullParser.END_TAG && "workgroup-properties".equals(parser.getName())) {
                    done = true;
                }
            }

            return props;
        }
    }
}
