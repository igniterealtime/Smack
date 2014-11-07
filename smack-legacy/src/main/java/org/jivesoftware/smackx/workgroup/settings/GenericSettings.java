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

import org.jivesoftware.smackx.workgroup.util.ModelUtil;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GenericSettings extends IQ {

    private Map<String, String> map = new HashMap<String, String>();

    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }


    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "generic-metadata";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public GenericSettings() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.append(">");
        if (ModelUtil.hasLength(getQuery())) {
            buf.append("<query>" + getQuery() + "</query>");
        }
        return buf;
    }

    /**
     * Packet extension provider for SoundSetting Packets.
     */
    public static class InternalProvider extends IQProvider<GenericSettings> {

        @Override
        public GenericSettings parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            GenericSettings setting = new GenericSettings();

            boolean done = false;


            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("entry".equals(parser.getName()))) {
                    eventType = parser.next();
                    String name = parser.nextText();
                    eventType = parser.next();
                    String value = parser.nextText();
                    setting.getMap().put(name, value);
                }
                else if (eventType == XmlPullParser.END_TAG && ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }

            return setting;
        }
    }


}

