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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ChatSettings extends IQ {

    /**
     * Defined as image type.
     */
    public static final int IMAGE_SETTINGS = 0;

    /**
     * Defined as Text settings type.
     */
    public static final int TEXT_SETTINGS = 1;

    /**
     * Defined as Bot settings type.
     */
    public static final int BOT_SETTINGS = 2;

    private List<ChatSetting> settings;
    private String key;
    private int type = -1;

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "chat-settings";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public ChatSettings() {
        super(ELEMENT_NAME, NAMESPACE);
        settings = new ArrayList<ChatSetting>();
    }

    public ChatSettings(String key) {
        this();
        setKey(key);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void addSetting(ChatSetting setting) {
        settings.add(setting);
    }

    public Collection<ChatSetting> getSettings() {
        return settings;
    }

    public ChatSetting getChatSetting(String key) {
        Collection<ChatSetting> col = getSettings();
        if (col != null) {
            Iterator<ChatSetting> iter = col.iterator();
            while (iter.hasNext()) {
                ChatSetting chatSetting = iter.next();
                if (chatSetting.getKey().equals(key)) {
                    return chatSetting;
                }
            }
        }
        return null;
    }

    public ChatSetting getFirstEntry() {
        if (settings.size() > 0) {
            return settings.get(0);
        }
        return null;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        if (key != null) {
            buf.append(" key=\"" + key + "\"");
        }

        if (type != -1) {
            buf.append(" type=\"" + type + "\"");
        }
        buf.setEmptyElement();
        return buf;
    }

    /**
     * Packet extension provider for AgentStatusRequest packets.
     */
    public static class InternalProvider extends IQProvider<ChatSettings> {

        @Override
        public ChatSettings parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            ChatSettings chatSettings = new ChatSettings();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("chat-setting".equals(parser.getName()))) {
                    chatSettings.addSetting(parseChatSetting(parser));

                }
                else if (eventType == XmlPullParser.END_TAG && ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
            return chatSettings;
        }

        private static ChatSetting parseChatSetting(XmlPullParser parser) throws XmlPullParserException, IOException {

            boolean done = false;
            String key = null;
            String value = null;
            int type = 0;

            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("key".equals(parser.getName()))) {
                    key = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("value".equals(parser.getName()))) {
                    value = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("type".equals(parser.getName()))) {
                    type = Integer.parseInt(parser.nextText());
                }
                else if (eventType == XmlPullParser.END_TAG && "chat-setting".equals(parser.getName())) {
                    done = true;
                }
            }
            return new ChatSetting(key, value, type);
        }
    }
}

