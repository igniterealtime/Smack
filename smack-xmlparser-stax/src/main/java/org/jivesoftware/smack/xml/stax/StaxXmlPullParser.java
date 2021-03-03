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

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public final class StaxXmlPullParser implements XmlPullParser {

    private final XMLStreamReader xmlStreamReader;

    private int depth;

    StaxXmlPullParser(XMLStreamReader xmlStreamReader) {
        this.xmlStreamReader = xmlStreamReader;
    }

    @Override
    public Object getProperty(String name) {
        return xmlStreamReader.getProperty(name);
    }

    @Override
    public String getInputEncoding() {
        return xmlStreamReader.getEncoding();
    }

    @Override
    public int getNamespaceCount() {
        return xmlStreamReader.getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int pos) {
        return xmlStreamReader.getNamespacePrefix(pos);
    }

    @Override
    public String getNamespaceUri(int pos) {
        return xmlStreamReader.getNamespaceURI(pos);
    }

    @Override
    public String getNamespace(String prefix) {
        if (prefix == null) {
            prefix = XMLConstants.DEFAULT_NS_PREFIX;
        }
        NamespaceContext namespaceContext = xmlStreamReader.getNamespaceContext();
        return namespaceContext.getNamespaceURI(prefix);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public String getPositionDescription() {
        Location location = xmlStreamReader.getLocation();
        return location.toString();
    }

    @Override
    public int getLineNumber() {
        Location location = xmlStreamReader.getLocation();
        return location.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        Location location = xmlStreamReader.getLocation();
        return location.getColumnNumber();
    }

    @Override
    public boolean isWhiteSpace() {
        return xmlStreamReader.isWhiteSpace();
    }

    @Override
    public String getText() {
        return xmlStreamReader.getText();
    }

    @Override
    public String getNamespace() {
        String prefix = getPrefix();
        return getNamespace(prefix);
    }

    @Override
    public String getName() {
        QName qname = getQName();
        return qname.getLocalPart();
    }

    @Override
    public QName getQName() {
        return xmlStreamReader.getName();
    }

    @Override
    public String getPrefix() {
        return xmlStreamReader.getPrefix();
    }

    @Override
    public int getAttributeCount() {
        return xmlStreamReader.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return xmlStreamReader.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        QName qname = getAttributeQName(index);
        if (qname == null) {
            return null;
        }
        return qname.getLocalPart();
    }

    @Override
    public QName getAttributeQName(int index) {
        return xmlStreamReader.getAttributeName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return xmlStreamReader.getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(int index) {
        return xmlStreamReader.getAttributeType(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return xmlStreamReader.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        String namespaceURI = namespace;
        String localName = name;
        return xmlStreamReader.getAttributeValue(namespaceURI, localName);
    }

    @Override
    public Event getEventType() {
        int staxEventInt = xmlStreamReader.getEventType();
        return staxEventIntegerToEvent(staxEventInt);
    }

    private boolean delayedDepthDecrement;

    @Override
    public Event next() throws XmlPullParserException {
        preNextEvent();

        int staxEventInt;
        try {
            staxEventInt = xmlStreamReader.next();
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e);
        }

        Event event = staxEventIntegerToEvent(staxEventInt);
        switch (event) {
        case START_ELEMENT:
            depth++;
            break;
        case END_ELEMENT:
            delayedDepthDecrement = true;
            break;
        default:
            // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
            break;
        }
        return event;
    }

    @Override
    public String nextText() throws IOException, XmlPullParserException {
        final String nextText;
        try {
            nextText = xmlStreamReader.getElementText();
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e);
        }

        // XMLStreamReader.getElementText() will forward to the next END_ELEMENT, hence we need to set
        // delayedDepthDecrement to true.
        delayedDepthDecrement = true;

        return nextText;
    }

    @Override
    public TagEvent nextTag() throws IOException, XmlPullParserException {
        preNextEvent();

        int staxEventInt;
        try {
            staxEventInt = xmlStreamReader.nextTag();
        } catch (XMLStreamException e) {
            throw new XmlPullParserException(e);
        }

        switch (staxEventInt) {
        case XMLStreamConstants.START_ELEMENT:
            depth++;
            return TagEvent.START_ELEMENT;
        case XMLStreamConstants.END_ELEMENT:
            delayedDepthDecrement = true;
            return TagEvent.END_ELEMENT;
        default:
            throw new AssertionError();
        }
    }

    private void preNextEvent() {
        if (delayedDepthDecrement) {
            depth--;
            delayedDepthDecrement = false;
            assert depth >= 0;
        }
    }

    private static Event staxEventIntegerToEvent(int staxEventInt) {
        switch (staxEventInt) {
        case XMLStreamConstants.START_ELEMENT:
            return Event.START_ELEMENT;
        case XMLStreamConstants.END_ELEMENT:
            return Event.END_ELEMENT;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            return Event.PROCESSING_INSTRUCTION;
        case XMLStreamConstants.CHARACTERS:
            return Event.TEXT_CHARACTERS;
        case XMLStreamConstants.COMMENT:
            return Event.COMMENT;
        case XMLStreamConstants.SPACE:
            return Event.IGNORABLE_WHITESPACE;
        case XMLStreamConstants.START_DOCUMENT:
            return Event.START_DOCUMENT;
        case XMLStreamConstants.END_DOCUMENT:
            return Event.END_DOCUMENT;
        case XMLStreamConstants.ENTITY_REFERENCE:
            return Event.ENTITY_REFERENCE;
        case XMLStreamConstants.ATTRIBUTE:
            return Event.OTHER;
        case XMLStreamConstants.DTD:
            return Event.OTHER;
        case XMLStreamConstants.CDATA:
            return Event.OTHER;
        case XMLStreamConstants.NAMESPACE:
            return Event.OTHER;
        case XMLStreamConstants.NOTATION_DECLARATION:
            return Event.OTHER;
        case XMLStreamConstants.ENTITY_DECLARATION:
            return Event.OTHER;
        default:
            throw new IllegalArgumentException("Unknown Stax event integer: " + staxEventInt);
        }
    }

    @Override
    public boolean supportsRoundtrip() {
        // TODO: Is there a StAX parser implementation which does support roundtrip?
        return false;
    }
}
