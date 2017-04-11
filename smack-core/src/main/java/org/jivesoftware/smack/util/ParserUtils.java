/**
 *
 * Copyright © 2014-2017 Florian Schmaus
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
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.jivesoftware.smack.SmackException;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ParserUtils {

    /**
     * The constant String "jid".
     */
    public static final String JID = "jid";

    public static void assertAtStartTag(XmlPullParser parser) throws XmlPullParserException {
        assert(parser.getEventType() == XmlPullParser.START_TAG);
    }

    public static void assertAtStartTag(XmlPullParser parser, String name) throws XmlPullParserException {
        assertAtStartTag(parser);
        assert name.equals(parser.getName());
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

    public static Jid getJidAttribute(XmlPullParser parser) throws XmppStringprepException {
        return getJidAttribute(parser, JID);
    }

    public static Jid getJidAttribute(XmlPullParser parser, String name) throws XmppStringprepException {
        final String jidString = parser.getAttributeValue("", name);
        if (jidString == null) {
            return null;
        }
        return JidCreate.from(jidString);
    }

    public static EntityBareJid getBareJidAttribute(XmlPullParser parser) throws XmppStringprepException {
        return getBareJidAttribute(parser, JID);
    }

    public static EntityBareJid getBareJidAttribute(XmlPullParser parser, String name) throws XmppStringprepException {
        final String jidString = parser.getAttributeValue("", name);
        if (jidString == null) {
            return null;
        }
        return JidCreate.entityBareFrom(jidString);
    }

    public static EntityFullJid getFullJidAttribute(XmlPullParser parser) throws XmppStringprepException {
        return getFullJidAttribute(parser, JID);
    }

    public static EntityFullJid getFullJidAttribute(XmlPullParser parser, String name) throws XmppStringprepException {
        final String jidString = parser.getAttributeValue("", name);
        if (jidString == null) {
            return null;
        }
        return JidCreate.entityFullFrom(jidString);
    }

    public static EntityJid getEntityJidAttribute(XmlPullParser parser, String name) throws XmppStringprepException {
        final String jidString = parser.getAttributeValue("", name);
        if (jidString == null) {
            return null;
        }
        Jid jid = JidCreate.from(jidString);

        if (!jid.hasLocalpart()) return null;

        EntityFullJid fullJid = jid.asEntityFullJidIfPossible();
        if (fullJid != null) {
            return fullJid;
        }

        EntityBareJid bareJid = jid.asEntityBareJidIfPossible();
        return bareJid;
    }

    public static Resourcepart getResourcepartAttribute(XmlPullParser parser, String name) throws XmppStringprepException {
        final String resourcepartString = parser.getAttributeValue("", name);
        if (resourcepartString == null) {
            return null;
        }
        return Resourcepart.from(resourcepartString);
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

    public static int getIntegerAttributeOrThrow(XmlPullParser parser, String name, String throwMessage) throws SmackException {
        Integer res = getIntegerAttribute(parser, name);
        if (res == null) {
            throw new SmackException(throwMessage);
        }
        return res;
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

    public static double getDoubleFromNextText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String doubleString = parser.nextText();
        return Double.valueOf(doubleString);
    }

    public static Double getDoubleAttribute(XmlPullParser parser, String name) {
        String valueString = parser.getAttributeValue("", name);
        if (valueString == null)
            return null;
        return Double.valueOf(valueString);
    }

    public static double getDoubleAttribute(XmlPullParser parser, String name, long defaultValue) {
        Double d = getDoubleAttribute(parser, name);
        if (d == null) {
            return defaultValue;
        }
        else {
            return d;
        }
    }

    public static Date getDateFromNextText(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        String dateString = parser.nextText();
        return XmppDateTime.parseDate(dateString);
    }

    public static URI getUriFromNextText(XmlPullParser parser) throws XmlPullParserException, IOException, URISyntaxException  {
        String uriString = parser.nextText();
        URI uri = new URI(uriString);
        return uri;
    }

    public static String getRequiredAttribute(XmlPullParser parser, String name) throws IOException {
        String value = parser.getAttributeValue("", name);
        if (StringUtils.isNullOrEmpty(value)) {
            throw new IOException("Attribute " + name + " is null or empty (" + value + ')');
        }
        return value;
    }

    public static String getRequiredNextText(XmlPullParser parser) throws XmlPullParserException, IOException {
        String text = parser.nextText();
        if (StringUtils.isNullOrEmpty(text)) {
            throw new IOException("Next text is null or empty (" + text + ')');
        }
        return text;
    }
}
