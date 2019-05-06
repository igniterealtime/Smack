/**
 *
 * Copyright 2016-2019 Florian Schmaus
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
package org.jivesoftware.smackx.iot.discovery.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.iot.discovery.element.IoTRegister;
import org.jivesoftware.smackx.iot.discovery.element.Tag;
import org.jivesoftware.smackx.iot.element.NodeInfo;
import org.jivesoftware.smackx.iot.parser.NodeInfoParser;

public class IoTRegisterProvider extends IQProvider<IoTRegister> {

    @Override
    public IoTRegister parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        boolean selfOwned = ParserUtils.getBooleanAttribute(parser, "selfOwned", false);
        NodeInfo nodeInfo = NodeInfoParser.parse(parser);
        List<Tag> tags = new ArrayList<>();
        while (parser.getDepth() != initialDepth) {
            XmlPullParser.Event event = parser.next();
            if (event != XmlPullParser.Event.START_ELEMENT) {
                continue;
            }
            final String element = parser.getName();
            Tag.Type type = null;
            switch (element) {
            case "str":
                type = Tag.Type.str;
                break;
            case "num":
                type = Tag.Type.num;
                break;
            }
            if (type == null) {
                continue;
            }
            String name = parser.getAttributeValue(null, "name");
            String value = parser.getAttributeValue(null, "value");
            tags.add(new Tag(name, type, value));
        }
        return new IoTRegister(tags, nodeInfo, selfOwned);
    }

}
