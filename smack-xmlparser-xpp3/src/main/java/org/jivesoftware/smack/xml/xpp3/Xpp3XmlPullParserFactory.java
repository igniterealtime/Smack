/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.xml.xpp3;

import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smack.xml.XmlPullParserFactory;

public class Xpp3XmlPullParserFactory implements XmlPullParserFactory {

    private static final Logger LOGGER = Logger.getLogger(Xpp3XmlPullParserFactory.class.getName());

    private static final org.xmlpull.v1.XmlPullParserFactory XPP3_XML_PULL_PARSER_FACTORY;

    public static final String FEATURE_XML_ROUNDTRIP = "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";

    /**
     * True if the XmlPullParser supports the XML_ROUNDTRIP feature.
     */
    public static final boolean XML_PULL_PARSER_SUPPORTS_ROUNDTRIP;

    static {
        org.xmlpull.v1.XmlPullParser xmlPullParser;
        boolean roundtrip = false;
        try {
            XPP3_XML_PULL_PARSER_FACTORY = org.xmlpull.v1.XmlPullParserFactory.newInstance();
            xmlPullParser = XPP3_XML_PULL_PARSER_FACTORY.newPullParser();
            try {
                xmlPullParser.setFeature(FEATURE_XML_ROUNDTRIP, true);
                // We could successfully set the feature
                roundtrip = true;
            } catch (org.xmlpull.v1.XmlPullParserException e) {
                // Doesn't matter if FEATURE_XML_ROUNDTRIP isn't available
                LOGGER.log(Level.FINEST, "XmlPullParser does not support XML_ROUNDTRIP", e);
            }
        }
        catch (org.xmlpull.v1.XmlPullParserException e) {
            // Something really bad happened
            throw new AssertionError(e);
        }
        XML_PULL_PARSER_SUPPORTS_ROUNDTRIP = roundtrip;
    }

    @Override
    public Xpp3XmlPullParser newXmlPullParser(Reader reader) throws XmlPullParserException {
        org.xmlpull.v1.XmlPullParser xpp3XmlPullParser;
        try {
            xpp3XmlPullParser = XPP3_XML_PULL_PARSER_FACTORY.newPullParser();
            xpp3XmlPullParser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            xpp3XmlPullParser.setInput(reader);
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }

        if (XML_PULL_PARSER_SUPPORTS_ROUNDTRIP) {
            try {
                xpp3XmlPullParser.setFeature(FEATURE_XML_ROUNDTRIP, true);
            }
            catch (org.xmlpull.v1.XmlPullParserException e) {
                LOGGER.log(Level.SEVERE,
                                "XmlPullParser does not support XML_ROUNDTRIP, although it was first determined to be supported",
                                e);
            }
        }

        return new Xpp3XmlPullParser(xpp3XmlPullParser);
    }

}
