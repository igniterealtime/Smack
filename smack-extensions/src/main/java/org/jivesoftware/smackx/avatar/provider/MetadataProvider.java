/**
 *
 * Copyright 2017 Fernando Ramirez, 2019 Paul Schaub
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
package org.jivesoftware.smackx.avatar.provider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.avatar.MetadataInfo;
import org.jivesoftware.smackx.avatar.MetadataPointer;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;

/**
 * User Avatar metadata provider class.
 *
 * @author Fernando Ramirez
 * @author Paul Schaub
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataProvider extends ExtensionElementProvider<MetadataExtension> {

    @Override
    public MetadataExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException {
        List<MetadataInfo> metadataInfos = null;
        List<MetadataPointer> pointers = null;

        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();

            switch (eventType) {
                case START_ELEMENT:
                    if (parser.getName().equals("info")) {
                        if (metadataInfos == null) {
                            metadataInfos = new ArrayList<>();
                        }

                        MetadataInfo info = parseInfo(parser);
                        if (info.getId() != null) {
                            metadataInfos.add(info);
                        }
                    }

                    if (parser.getName().equals("pointer")) {
                        if (pointers == null) {
                            pointers = new ArrayList<>();
                        }

                        pointers.add(parsePointer(parser));
                    }
                    break;
                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
            }
        }
        return new MetadataExtension(metadataInfos, pointers);
    }

    private static MetadataInfo parseInfo(XmlPullParser parser) throws XmlPullParserException {
        String id;
        URL url = null;
        long bytes = 0;
        String type;
        int pixelsHeight = 0;
        int pixelsWidth = 0;

        id = parser.getAttributeValue("", "id");
        type = parser.getAttributeValue("", "type");
        String urlString = parser.getAttributeValue("", "url");
        if (urlString != null && !urlString.isEmpty()) {
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                throw new XmlPullParserException("Cannot parse URL '" + urlString + "'");
            }
        }

        String bytesString = parser.getAttributeValue("", "bytes");
        if (bytesString != null) {
            bytes = Long.parseLong(bytesString);
        }

        String widthString = parser.getAttributeValue("", "width");
        if (widthString != null) {
            pixelsWidth = Integer.parseInt(widthString);
        }

        String heightString = parser.getAttributeValue("", "height");
        if (heightString != null) {
            pixelsHeight = Integer.parseInt(heightString);
        }

        try {
            return new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);
        } catch (IllegalArgumentException e) {
            throw new XmlPullParserException(e);
        }
    }

    private static MetadataPointer parsePointer(XmlPullParser parser) throws XmlPullParserException, IOException {
        int pointerDepth = parser.getDepth();
        String namespace = null;
        HashMap<String, Object> fields = null;

        outperloop: while (true) {
            XmlPullParser.TagEvent tag = parser.nextTag();

            switch (tag) {
                case START_ELEMENT:
                    if (parser.getName().equals("x")) {
                        namespace = parser.getNamespace();
                    } else {
                        if (fields == null) {
                            fields = new HashMap<>();
                        }

                        String name = parser.getName();
                        Object value = parser.nextText();
                        fields.put(name, value);
                    }
                    break;

                case END_ELEMENT:
                    if (parser.getDepth() == pointerDepth) {
                        break outperloop;
                    }
            }
        }

        return new MetadataPointer(namespace, fields);
    }

}
