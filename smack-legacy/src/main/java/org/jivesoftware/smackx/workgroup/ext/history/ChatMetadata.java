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

package org.jivesoftware.smackx.workgroup.ext.history;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

public class ChatMetadata extends IQ {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "chat-metadata";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";


    private String sessionID;

    public ChatMetadata() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }


    private Map<String, List<String>> map = new HashMap<String, List<String>>();

    public void setMetadata(Map<String, List<String>> metadata) {
        this.map = metadata;
    }

    public Map<String, List<String>> getMetadata() {
        return map;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        buf.append("<sessionID>").append(getSessionID()).append("</sessionID>");

        return buf;
    }

    /**
     * An IQProvider for Metadata packets.
     *
     * @author Derek DeMoro
     */
    public static class Provider extends IqProvider<ChatMetadata> {

        @Override
        public ChatMetadata parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                        throws XmlPullParserException, IOException {
            final ChatMetadata chatM = new ChatMetadata();

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if (parser.getName().equals("sessionID")) {
                       chatM.setSessionID(parser.nextText());
                    }
                    else if (parser.getName().equals("metadata")) {
                        Map<String, List<String>> map = MetaDataUtils.parseMetaData(parser);
                        chatM.setMetadata(map);
                    }
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return chatM;
        }
    }
}




