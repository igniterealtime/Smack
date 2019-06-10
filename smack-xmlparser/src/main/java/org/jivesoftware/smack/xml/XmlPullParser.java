/**
 *
 * Copyright 2019 Florian Schmaus.
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
package org.jivesoftware.smack.xml;

import java.io.IOException;

import javax.xml.namespace.QName;

/**
 * Smack's interface for XML pull parsers. The main XML parser implementations are "Xml Pull Parser 3" (XPP3) on Android and "Streaming API for XML" (StAX, JSR 173) on Java.
 *
 * <p>
 * Differences from StAX's XMLStreamReader are:
 * </p>
 * <ul>
 *  <li>{@link #getName()} and {@link #getAttributeName(int)} return localname, there is {@link #getQName()} and {@link #getAttributeQName(int)} to retrieve the qualified name ({@link javax.xml.namespace.QName QName}).</li>
 *  <li>{@link #nextText()} instead of {@code XMLStreamReader.getElementText()}.
 * </ul>
 * <p>
 * Differences from XPP3's XmlPullParser are:
 * </p>
 * <ul>
 *  <li>Methods taking an attribute, like {@link #getAttributeName(int)} index return <code>null</code> instead of throwing an exception if no attribute with the given index exists.</li>
 * </ul>
 *
 * <h2>Developer Information</h2>
 * <p>
 * The following table shows the mapping of Smack's XmlPullParser events to StAX and XPP3 events:
 * </p>
 * <table summary="XmlPullParser event mapping">
 * <tr><th>Smack's {@link XmlPullParser.Event}</th><th>StAX Event</th><th>XPP3 Event</th></tr>
 * <tr><td>{@link XmlPullParser.Event#START_DOCUMENT}</td><td>START_DOCUMENT (7)</td><td>START_DOCUMENT (0)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#END_DOCUMENT}</td><td>END_DOCUMENT (8)</td><td>END_DOCUMENT (1)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#START_ELEMENT}</td><td>START_ELEMENT (1)</td><td>START_TAG (2)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#END_ELEMENT}</td><td>END_ELEMENT (2)</td><td>END_TAG (3)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#TEXT_CHARACTERS}</td><td>CHARACTERS (4)</td><td>TEXT (4)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#PROCESSING_INSTRUCTION}</td><td>PROCESSING_INSTRUCTION (3)</td><td>PROCESSING_INSTRUCTION (8)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#COMMENT}</td><td>COMMENT (5)</td><td>COMMENT (9)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#IGNORABLE_WHITESPACE}</td><td>SPACE (6)</td><td>IGNORABLE_WHITESPACE (7)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#ENTITY_REFERENCE}</td><td>ENTITY_REFERENCE (9)</td><td>ENTITY_REF (6)</td></tr>
 * <tr><td>{@link XmlPullParser.Event#OTHER}</td><td>ENTITY_REFERENCE (9)</td><td>ENTITY_REF (6)</td></tr>
 * </table>
 * <p>{@link XmlPullParser.Event#OTHER} includes
 * in case of StAX: ATTRIBUTE (10), DTD (11), CDATA (12), NAMESPACE (13), NOTATION_DECLARATION (14) and ENTITY_DECLRATION (15),
 * in case of XPP3: CDSECT (5), DOCDECL (10).
 * </p>
 *
 */
public interface XmlPullParser {

    Object getProperty(String name);

    String getInputEncoding();

    int getNamespaceCount() throws XmlPullParserException;

    String getNamespacePrefix(int pos) throws XmlPullParserException;

    String getNamespaceUri(int pos) throws XmlPullParserException;

    String getNamespace(String prefix);

    int getDepth();

    String getPositionDescription();

    int getLineNumber();

    int getColumnNumber();

    boolean isWhiteSpace() throws XmlPullParserException;

    String getText();

    String getNamespace();

    /**
     * Return the name for the current START_ELEMENT or END_ELEMENT event. This method must only be called if the
     * current event is START_ELEMENT or END_ELEMENT.
     *
     * @return the name for the current START_ELEMETN or END_ELEMENT event.
     */
    String getName();

    QName getQName();

    String getPrefix();

    int getAttributeCount();

    String getAttributeNamespace(int index);

    /**
     * Returns the loacalpart of the attribute's name or <code>null</code> in case the index does not refer to an
     * attribute.
     *
     * @param index the attribute index.
     * @return the localpart of the attribute's name or <code>null</code>.
     */
    String getAttributeName(int index);

    QName getAttributeQName(int index);

    String getAttributePrefix(int index);

    String getAttributeType(int index);

    String getAttributeValue(int index);

    String getAttributeValue(String namespace, String name);

    default String getAttributeValue(String name) {
        return getAttributeValue(null, name);
    }

    Event getEventType() throws XmlPullParserException;

    Event next() throws IOException, XmlPullParserException;

    /**
     * Reads the content of a text-only element, an exception is thrown if this is
     * not a text-only element.
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding END_ELEMENT.</li>
     * </ul>
     *
     * @return the textual content of the current element.
     * @throws IOException in case of an IO error.
     * @throws XmlPullParserException in case of an XML pull parser error.
     */
    String nextText() throws IOException, XmlPullParserException;

    TagEvent nextTag() throws IOException, XmlPullParserException;

    enum TagEvent {
        START_ELEMENT,
        END_ELEMENT,
    }

    enum Event {
        START_DOCUMENT,
        END_DOCUMENT,
        START_ELEMENT,
        END_ELEMENT,

        /**
         * Replaces TEXT from XPP3.
         */
        TEXT_CHARACTERS,
        PROCESSING_INSTRUCTION,
        COMMENT,
        IGNORABLE_WHITESPACE,
        ENTITY_REFERENCE,
        OTHER,
    }

    boolean supportsRoundtrip();
}
