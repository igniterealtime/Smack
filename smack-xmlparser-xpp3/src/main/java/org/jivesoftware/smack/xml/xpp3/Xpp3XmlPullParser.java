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

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public final class Xpp3XmlPullParser implements XmlPullParser {

    private final org.xmlpull.v1.XmlPullParser xpp3XmlPullParser;

    public Xpp3XmlPullParser(org.xmlpull.v1.XmlPullParser xpp3XmlPullParser) {
        this.xpp3XmlPullParser = xpp3XmlPullParser;
    }

    @Override
    public Object getProperty(String name) {
        return xpp3XmlPullParser.getProperty(name);
    }

    @Override
    public String getInputEncoding() {
        return xpp3XmlPullParser.getInputEncoding();
    }

    @Override
    public int getNamespaceCount() throws XmlPullParserException {
        int depth = xpp3XmlPullParser.getDepth();
        try {
            return xpp3XmlPullParser.getNamespaceCount(depth);
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        try {
            return xpp3XmlPullParser.getNamespacePrefix(pos);
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        try {
            return xpp3XmlPullParser.getNamespaceUri(pos);
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
    }

    @Override
    public String getNamespace(String prefix) {
        return xpp3XmlPullParser.getNamespace(prefix);
    }

    @Override
    public int getDepth() {
        return xpp3XmlPullParser.getDepth();
    }

    @Override
    public String getPositionDescription() {
        return xpp3XmlPullParser.getPositionDescription();
    }

    @Override
    public int getLineNumber() {
        return xpp3XmlPullParser.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return xpp3XmlPullParser.getColumnNumber();
    }

    @Override
    public boolean isWhiteSpace() throws XmlPullParserException {
        try {
            return xpp3XmlPullParser.isWhitespace();
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
    }

    @Override
    public String getText() {
        return xpp3XmlPullParser.getText();
    }

    @Override
    public String getNamespace() {
        return xpp3XmlPullParser.getNamespace();
    }

    @Override
    public String getName() {
        return xpp3XmlPullParser.getName();
    }

    @Override
    public QName getQName() {
        String localpart = xpp3XmlPullParser.getName();
        String prefix = xpp3XmlPullParser.getPrefix();
        prefix = nullValueToDefaultPrefix(prefix);
        String namespace = xpp3XmlPullParser.getNamespace();

        return new QName(namespace, localpart, prefix);
    }

    @Override
    public String getPrefix() {
        return xpp3XmlPullParser.getPrefix();
    }

    @Override
    public int getAttributeCount() {
        return xpp3XmlPullParser.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        String namespace;
        try {
            namespace = xpp3XmlPullParser.getAttributeNamespace(index);
        }
        catch (IndexOutOfBoundsException e) {
            return null;
        }

        if (XMLConstants.NULL_NS_URI.equals(namespace)) {
            namespace = null;
        }
        return namespace;
    }

    @Override
    public String getAttributeName(int index) {
        try {
            return xpp3XmlPullParser.getAttributeName(index);
        }
        catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public QName getAttributeQName(int index) {
        String localpart = getAttributeName(index);
        if (localpart == null) {
            return null;
        }
        String prefix = getAttributePrefix(index);
        prefix = nullValueToDefaultPrefix(prefix);
        String namespace = getAttributeNamespace(index);
        return new QName(namespace, localpart, prefix);
    }

    @Override
    public String getAttributePrefix(int index) {
        String prefix;
        try {
            prefix = xpp3XmlPullParser.getAttributePrefix(index);
        }
        catch (IndexOutOfBoundsException e) {
            return null;
        }
        prefix = nullValueToDefaultPrefix(prefix);
        return prefix;
    }

    @Override
    public String getAttributeType(int index) {
        try {
            return xpp3XmlPullParser.getAttributeType(index);
        }
        catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public String getAttributeValue(int index) {
        return xpp3XmlPullParser.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return xpp3XmlPullParser.getAttributeValue(namespace, name);
    }

    @Override
    public Event getEventType() throws XmlPullParserException {
        int xpp3EventInt;
        try {
            xpp3EventInt = xpp3XmlPullParser.getEventType();
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
        return xpp3EventIntegerToEvent(xpp3EventInt);
    }

    @Override
    public Event next() throws IOException, XmlPullParserException {
        int xpp3EventInt;
        try {
            xpp3EventInt = xpp3XmlPullParser.next();
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
        return xpp3EventIntegerToEvent(xpp3EventInt);
    }

    @Override
    public String nextText() throws IOException, XmlPullParserException {
        try {
            return xpp3XmlPullParser.nextText();
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
    }

    @Override
    public TagEvent nextTag() throws IOException, XmlPullParserException {
        int xpp3EventInt;
        try {
            xpp3EventInt = xpp3XmlPullParser.nextTag();
        } catch (org.xmlpull.v1.XmlPullParserException e) {
            throw new XmlPullParserException(e);
        }
        switch (xpp3EventInt) {
        case org.xmlpull.v1.XmlPullParser.START_TAG:
            return TagEvent.START_ELEMENT;
        case org.xmlpull.v1.XmlPullParser.END_TAG:
            return TagEvent.END_ELEMENT;
        default:
            throw new AssertionError();
        }
    }

    @Override
    public boolean supportsRoundtrip() {
        return xpp3XmlPullParser.getFeature(Xpp3XmlPullParserFactory.FEATURE_XML_ROUNDTRIP);
    }

    private static Event xpp3EventIntegerToEvent(int xpp3EventInt) {
        switch (xpp3EventInt) {
        case org.xmlpull.v1.XmlPullParser.START_DOCUMENT:
            return Event.START_DOCUMENT;
        case org.xmlpull.v1.XmlPullParser.END_DOCUMENT:
            return Event.END_DOCUMENT;
        case org.xmlpull.v1.XmlPullParser.START_TAG:
            return Event.START_ELEMENT;
        case org.xmlpull.v1.XmlPullParser.END_TAG:
            return Event.END_ELEMENT;
        case org.xmlpull.v1.XmlPullParser.TEXT:
            return Event.TEXT_CHARACTERS;
        case org.xmlpull.v1.XmlPullParser.CDSECT:
            return Event.OTHER;
        case org.xmlpull.v1.XmlPullParser.ENTITY_REF:
            return Event.ENTITY_REFERENCE;
        case org.xmlpull.v1.XmlPullParser.IGNORABLE_WHITESPACE:
            return Event.IGNORABLE_WHITESPACE;
        case org.xmlpull.v1.XmlPullParser.PROCESSING_INSTRUCTION:
            return Event.PROCESSING_INSTRUCTION;
        case org.xmlpull.v1.XmlPullParser.COMMENT:
            return Event.COMMENT;
        case org.xmlpull.v1.XmlPullParser.DOCDECL:
            return Event.OTHER;
        default:
            throw new IllegalArgumentException("Unknown XPP3 event integer: " + xpp3EventInt);
        }
    }

    private static String nullValueToDefaultPrefix(String prefix) {
        if (prefix != null) {
            return prefix;
        }
        return XMLConstants.DEFAULT_NS_PREFIX;
    }
}
