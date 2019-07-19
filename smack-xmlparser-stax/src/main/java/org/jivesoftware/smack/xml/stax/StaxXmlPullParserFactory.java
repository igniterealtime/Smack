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
package org.jivesoftware.smack.xml.stax;

import java.io.Reader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smack.xml.XmlPullParserFactory;

public class StaxXmlPullParserFactory implements XmlPullParserFactory {

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        // XPP3 appears to coalescing hence we need to configure our StAX parser to also return all available text on
        // getText().
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // Internal and external entity references are prohibited in XMPP (RFC 6120 § 11.1).
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    }

    @Override
    public StaxXmlPullParser newXmlPullParser(Reader reader) throws XmlPullParserException {
        XMLStreamReader xmlStreamReader;
        try {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e);
        }
        return new StaxXmlPullParser(xmlStreamReader);
    }
}
