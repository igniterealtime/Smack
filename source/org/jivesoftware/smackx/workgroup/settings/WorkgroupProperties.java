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
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "workgroup-properties";

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
        if (ModelUtil.hasLength(getJid())) {
            buf.append("jid=\"" + getJid() + "\" ");
        }
        buf.append("></").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }

    /**
     * Packet extension provider for SoundSetting Packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            WorkgroupProperties props = new WorkgroupProperties();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("authRequired".equals(parser.getName()))) {
                    props.setAuthRequired(new Boolean(parser.nextText()).booleanValue());
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
