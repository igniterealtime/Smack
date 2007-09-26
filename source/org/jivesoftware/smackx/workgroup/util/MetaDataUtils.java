/**
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

package org.jivesoftware.smackx.workgroup.util;

import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

/**
 * Utility class for meta-data parsing and writing.
 *
 * @author Matt Tucker
 */
public class MetaDataUtils {

    /**
     * Parses any available meta-data and returns it as a Map of String name/value pairs. The
     * parser must be positioned at an opening meta-data tag, or the an empty map will be returned.
     *
     * @param parser the XML parser positioned at an opening meta-data tag.
     * @return the meta-data.
     * @throws XmlPullParserException if an error occurs while parsing the XML.
     * @throws IOException            if an error occurs while parsing the XML.
     */
    public static Map parseMetaData(XmlPullParser parser) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();

        // If correctly positioned on an opening meta-data tag, parse meta-data.
        if ((eventType == XmlPullParser.START_TAG)
                && parser.getName().equals(MetaData.ELEMENT_NAME)
                && parser.getNamespace().equals(MetaData.NAMESPACE)) {
            Map metaData = new Hashtable();

            eventType = parser.nextTag();

            // Keep parsing until we've gotten to end of meta-data.
            while ((eventType != XmlPullParser.END_TAG)
                    || (!parser.getName().equals(MetaData.ELEMENT_NAME))) {
                String name = parser.getAttributeValue(0);
                String value = parser.nextText();

                if (metaData.containsKey(name)) {
                    List values = (List)metaData.get(name);
                    values.add(value);
                }
                else {
                    List values = new ArrayList();
                    values.add(value);
                    metaData.put(name, values);
                }

                eventType = parser.nextTag();
            }

            return metaData;
        }

        return Collections.EMPTY_MAP;
    }

    /**
     * Serializes a Map of String name/value pairs into the meta-data XML format.
     *
     * @param metaData the Map of meta-data.
     * @return the meta-data values in XML form.
     */
    public static String serializeMetaData(Map metaData) {
        StringBuilder buf = new StringBuilder();
        if (metaData != null && metaData.size() > 0) {
            buf.append("<metadata xmlns=\"http://jivesoftware.com/protocol/workgroup\">");
            for (Iterator i = metaData.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                Object value = metaData.get(key);
                if (value instanceof List) {
                    List values = (List)metaData.get(key);
                    for (Iterator it = values.iterator(); it.hasNext();) {
                        String v = (String)it.next();
                        buf.append("<value name=\"").append(key).append("\">");
                        buf.append(StringUtils.escapeForXML(v));
                        buf.append("</value>");
                    }
                }
                else if (value instanceof String) {
                    buf.append("<value name=\"").append(key).append("\">");
                    buf.append(StringUtils.escapeForXML((String)value));
                    buf.append("</value>");
                }
            }
            buf.append("</metadata>");
        }
        return buf.toString();
    }
}
