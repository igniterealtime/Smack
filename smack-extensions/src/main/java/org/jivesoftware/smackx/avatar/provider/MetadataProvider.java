/**
 *
 * Copyright 2017 Fernando Ramirez
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.avatar.MetadataInfo;
import org.jivesoftware.smackx.avatar.MetadataPointer;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * User Avatar metedata provider class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataProvider extends ExtensionElementProvider<MetadataExtension> {

    @Override
    public MetadataExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        List<MetadataInfo> metadataInfos = null;
        List<MetadataPointer> pointers = null;

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

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

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new MetadataExtension(metadataInfos, pointers);
    }

    private MetadataInfo parseInfo(XmlPullParser parser) {
        String id = null;
        String url = null;
        long bytes = 0;
        String type = null;
        int pixelsHeight = 0;
        int pixelsWidth = 0;

        id = parser.getAttributeValue("", "id");
        url = parser.getAttributeValue("", "url");
        type = parser.getAttributeValue("", "type");

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

        return new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);
    }

    private MetadataPointer parsePointer(XmlPullParser parser) throws XmlPullParserException, IOException {
        int pointerDepth = parser.getDepth();
        String namespace = null;
        HashMap<String, Object> fields = null;

        outerloop2: while (true) {
            int eventType2 = parser.next();

            if (eventType2 == XmlPullParser.START_TAG) {
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
            } else if (eventType2 == XmlPullParser.END_TAG) {
                if (parser.getDepth() == pointerDepth) {
                    break outerloop2;
                }
            }
        }

        return new MetadataPointer(namespace, fields);
    }

}
