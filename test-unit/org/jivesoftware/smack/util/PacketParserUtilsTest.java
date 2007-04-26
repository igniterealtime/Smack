/**
 * $Revision:$
 * $Date:$
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smack.util;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

/**
 *
 */
public class PacketParserUtilsTest {
    @Test
    public void multipleMessageBodiesParsingTest() throws Exception {
        final String messageBody1 = "This is a test of the emergency broadcast system, 1.";
        final String lang2 = "ru";
        final String messageBody2 = "This is a test of the emergency broadcast system, 2.";
        final String lang3 = "sp";
        final String messageBody3 = "This is a test of the emergency broadcast system, 3.";

        StringBuilder controlBuilder = new StringBuilder();
        controlBuilder.append("<message xml:lang=\"en\">")
                .append("<body>")
                .append(messageBody1)
                .append("</body>")
                .append("<body xml:lang=\"")
                .append(lang2)
                .append("\">")
                .append(messageBody2)
                .append("</body>")
                .append("<body xml:lang=\"")
                .append(lang3)
                .append("\">")
                .append(messageBody3)
                .append("</body>")
                .append("</message>");
        String control = controlBuilder.toString();

        Packet message = PacketParserUtils.parseMessage(getParser(control));
        assertXMLEqual(control, message.toXML());
    }

    private XmlPullParser getParser(String control) throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();
        parser.setInput(new StringReader(control));
        while(true) {
            if(parser.next() == XmlPullParser.START_TAG
                    && parser.getName().equals("message")) { break; }
        }
        return parser;
    }
}
