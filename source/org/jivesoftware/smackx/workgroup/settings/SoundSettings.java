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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

public class SoundSettings extends IQ {
    private String outgoingSound;
    private String incomingSound;


    public void setOutgoingSound(String outgoingSound) {
        this.outgoingSound = outgoingSound;
    }

    public void setIncomingSound(String incomingSound) {
        this.incomingSound = incomingSound;
    }

    public byte[] getIncomingSoundBytes() {
        return StringUtils.decodeBase64(incomingSound);
    }

    public byte[] getOutgoingSoundBytes() {
        return StringUtils.decodeBase64(outgoingSound);
    }


    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "sound-settings";

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
     * Packet extension provider for SoundSetting Packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            SoundSettings soundSettings = new SoundSettings();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("outgoingSound".equals(parser.getName()))) {
                    soundSettings.setOutgoingSound(parser.nextText());
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("incomingSound".equals(parser.getName()))) {
                    soundSettings.setIncomingSound(parser.nextText());
                }
                else if (eventType == XmlPullParser.END_TAG && "sound-settings".equals(parser.getName())) {
                    done = true;
                }
            }

            return soundSettings;
        }
    }
}

