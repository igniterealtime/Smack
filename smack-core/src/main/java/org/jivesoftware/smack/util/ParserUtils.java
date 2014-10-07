/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.io.IOException;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ParserUtils {
    public static void assertAtStartTag(XmlPullParser parser) throws XmlPullParserException {
        assert(parser.getEventType() == XmlPullParser.START_TAG);
    }

    public static void assertAtEndTag(XmlPullParser parser) throws XmlPullParserException {
        assert(parser.getEventType() == XmlPullParser.END_TAG);
    }

    public static void forwardToEndTagOfDepth(XmlPullParser parser, int depth)
                    throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (!(event == XmlPullParser.END_TAG && parser.getDepth() == depth)) {
            event = parser.next();
        }
    }

    /**
     * Get the boolean value of an argument.
     * 
     * @param parser
     * @param name
     * @return the boolean value or null of no argument of the given name exists
     */
    public static Boolean getBooleanAttribute(XmlPullParser parser, String name) {
        String valueString = parser.getAttributeValue("", name);
        if (valueString == null)
            return null;
        valueString = valueString.toLowerCase(Locale.US);
        if (valueString.equals("true") || valueString.equals("0")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean getBooleanAttribute(XmlPullParser parser, String name,
                    boolean defaultValue) {
        Boolean bool = getBooleanAttribute(parser, name);
        if (bool == null) {
            return defaultValue;
        }
        else {
            return bool;
        }
    }

    public static Integer getIntegerAttribute(XmlPullParser parser, String name) {
        String valueString = parser.getAttributeValue("", name);
        if (valueString == null)
            return null;
        return Integer.valueOf(valueString);
    }

    public static int getIntegerAttribute(XmlPullParser parser, String name, int defaultValue) {
        Integer integer = getIntegerAttribute(parser, name);
        if (integer == null) {
            return defaultValue;
        }
        else {
            return integer;
        }
    }

    public static int getIntegerFromNextText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String intString = parser.nextText();
        return Integer.valueOf(intString);
    }

    public static Long getLongAttribute(XmlPullParser parser, String name) {
        String valueString = parser.getAttributeValue("", name);
        if (valueString == null)
            return null;
        return Long.valueOf(valueString);
    }

    public static long getLongAttribute(XmlPullParser parser, String name, long defaultValue) {
        Long l = getLongAttribute(parser, name);
        if (l == null) {
            return defaultValue;
        }
        else {
            return l;
        }
    }
}
