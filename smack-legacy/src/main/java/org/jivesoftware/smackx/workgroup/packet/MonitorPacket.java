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
package org.jivesoftware.smackx.workgroup.packet;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MonitorPacket extends IQ {

    private String sessionID;

    private boolean isMonitor;

    public boolean isMonitor() {
        return isMonitor;
    }

    public void setMonitor(boolean monitor) {
        isMonitor = monitor;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Element name of the stanza(/packet) extension.
     */
    public static final String ELEMENT_NAME = "monitor";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public MonitorPacket() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();

        if (sessionID != null) {
            buf.append("<makeOwner sessionID=\""+sessionID+"\"></makeOwner>");
        }

        return buf;
    }


    /**
     * Stanza(/Packet) extension provider for Monitor Packets.
     */
    public static class InternalProvider extends IQProvider<MonitorPacket> {

        @Override
        public MonitorPacket parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            MonitorPacket packet = new MonitorPacket();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("isMonitor".equals(parser.getName()))) {
                    String value = parser.nextText();
                    if ("false".equalsIgnoreCase(value)) {
                        packet.setMonitor(false);
                    }
                    else {
                        packet.setMonitor(true);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG && "monitor".equals(parser.getName())) {
                    done = true;
                }
            }

            return packet;
        }
    }
}
