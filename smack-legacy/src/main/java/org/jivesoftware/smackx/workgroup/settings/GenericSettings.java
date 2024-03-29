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
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public class GenericSettings extends IQ {

    private Map<String, String> map = new HashMap<>();

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
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "generic-metadata";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public GenericSettings() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.append('>');
        if (StringUtils.isNotEmpty(getQuery())) {
            buf.append("<query>" + getQuery() + "</query>");
        }
        return buf;
    }

    /**
     * Stanza extension provider for SoundSetting Packets.
     */
    public static class InternalProvider extends IqProvider<GenericSettings> {

        @Override
        public GenericSettings parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
            GenericSettings setting = new GenericSettings();

            boolean done = false;


            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && "entry".equals(parser.getName())) {
                    eventType = parser.next();
                    String name = parser.nextText();
                    eventType = parser.next();
                    String value = parser.nextText();
                    setting.getMap().put(name, value);
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT && ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }

            return setting;
        }
    }


}

