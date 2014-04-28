/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;
import org.jivesoftware.smackx.shim.provider.HeaderProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract parent for Req and Resp packet providers.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public abstract class AbstractHttpOverXmppProvider implements IQProvider {

    private static final String ELEMENT_HEADERS = "headers";
    private static final String ELEMENT_HEADER = "header";
    private static final String ELEMENT_DATA = "data";
    private static final String ELEMENT_TEXT = "text";
    private static final String ELEMENT_BASE_64 = "base64";
    private static final String ELEMENT_CHUNKED_BASE_64 = "chunkedBase64";
    private static final String ELEMENT_XML = "xml";
    static final String ELEMENT_IBB = "ibb";
    static final String ELEMENT_SIPUB = "sipub";
    static final String ELEMENT_JINGLE = "jingle";

    private static final String ATTRIBUTE_STREAM_ID = "streamId";
    private static final String ATTRIBUTE_SID = "sid";
    static final String ATTRIBUTE_VERSION = "version";

    /**
     * Parses Headers and Data elements.
     *
     * @param parser      parser
     * @param elementName name of concrete implementation of this element
     * @param body        parent Body element
     * @throws Exception if anything goes wrong
     */
    protected void parseHeadersAndData(XmlPullParser parser, String elementName, AbstractHttpOverXmpp.AbstractBody body) throws Exception {
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(ELEMENT_HEADERS)) {
                    HeadersExtension headersExtension = parseHeaders(parser);
                    body.setHeaders(headersExtension);
                } else if (parser.getName().endsWith(ELEMENT_DATA)) {
                    AbstractHttpOverXmpp.Data data = parseData(parser);
                    body.setData(data);
                } else {
                    throw new IllegalArgumentException("unexpected tag:" + parser.getName() + "'");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(elementName)) {
                    done = true;
                }
            }
        }
    }

    private HeadersExtension parseHeaders(XmlPullParser parser) throws Exception {
        HeaderProvider provider = new HeaderProvider();
        Set<Header> set = new HashSet<Header>();
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(ELEMENT_HEADER)) {
                    Header header = (Header) provider.parseExtension(parser);
                    set.add(header);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(ELEMENT_HEADERS)) {
                    done = true;
                }
            }
        }
        return new HeadersExtension(set);
    }

    private AbstractHttpOverXmpp.Data parseData(XmlPullParser parser) throws Exception {
        AbstractHttpOverXmpp.DataChild child = null;
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(ELEMENT_TEXT)) {
                    child = parseText(parser);
                } else if (parser.getName().equals(ELEMENT_BASE_64)) {
                    child = parseBase64(parser);
                } else if (parser.getName().equals(ELEMENT_CHUNKED_BASE_64)) {
                    child = parseChunkedBase64(parser);
                } else if (parser.getName().equals(ELEMENT_XML)) {
                    child = parseXml(parser);
                } else if (parser.getName().equals(ELEMENT_IBB)) {
                    child = parseIbb(parser);
                } else if (parser.getName().equals(ELEMENT_SIPUB)) {
                    // TODO: sipub is allowed by xep-0332, but is not implemented yet
                    throw new UnsupportedOperationException("sipub is not supported yet");
                } else if (parser.getName().equals(ELEMENT_JINGLE)) {
                    // TODO: jingle is allowed by xep-0332, but is not implemented yet
                    throw new UnsupportedOperationException("jingle is not supported yet");
                } else {
                    // other elements are not allowed
                    throw new IllegalArgumentException("unsupported child tag: " + parser.getName());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(ELEMENT_DATA)) {
                    done = true;
                }
            }
        }

        AbstractHttpOverXmpp.Data data = new AbstractHttpOverXmpp.Data(child);
        return data;
    }

    private AbstractHttpOverXmpp.Text parseText(XmlPullParser parser) throws Exception {
        String text = null;
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(ELEMENT_TEXT)) {
                    done = true;
                } else {
                    throw new IllegalArgumentException("unexpected end tag of: " + parser.getName());
                }
            } else if (eventType == XmlPullParser.TEXT) {
                text = parser.getText();
            } else {
                throw new IllegalArgumentException("unexpected eventType: " + eventType);
            }
        }

        return new AbstractHttpOverXmpp.Text(text);
    }

    private AbstractHttpOverXmpp.Xml parseXml(XmlPullParser parser) throws Exception {
        StringBuilder builder = new StringBuilder();
        boolean done = false;
        boolean startClosed = true;

        while (!done) {
            int eventType = parser.next();

            if ((eventType == XmlPullParser.END_TAG) && parser.getName().equals(ELEMENT_XML)) {
                done = true;
            } else { // just write everything else as text

                if (eventType == XmlPullParser.START_TAG) {

                    if (!startClosed) {
                        builder.append('>');
                    }

                    builder.append('<');
                    builder.append(parser.getName());
                    appendXmlAttributes(parser, builder);
                    startClosed = false;
                } else if (eventType == XmlPullParser.END_TAG) {

                    if (startClosed) {
                        builder.append("</");
                        builder.append(parser.getName());
                        builder.append('>');
                    } else {
                        builder.append("/>");
                        startClosed = true;
                    }
                } else if (eventType == XmlPullParser.TEXT) {

                    if (!startClosed) {
                        builder.append('>');
                        startClosed = true;
                    }
                    builder.append(StringUtils.escapeForXML(parser.getText()));
                } else {
                    throw new IllegalArgumentException("unexpected eventType: " + eventType);
                }
            }
        }

        return new AbstractHttpOverXmpp.Xml(builder.toString());
    }

    private void appendXmlAttributes(XmlPullParser parser, StringBuilder builder) throws Exception {
        // NOTE: for now we ignore namespaces
        int count = parser.getAttributeCount();

        if (count > 0) {

            for (int i = 0; i < count; i++) {
                builder.append(' ');
                builder.append(parser.getAttributeName(i));
                builder.append("=\"");
                builder.append(StringUtils.escapeForXML(parser.getAttributeValue(i)));
                builder.append('"');
            }
        }
    }

    private AbstractHttpOverXmpp.Base64 parseBase64(XmlPullParser parser) throws Exception {
        String text = null;
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.END_TAG) {

                if (parser.getName().equals(ELEMENT_BASE_64)) {
                    done = true;
                } else {
                    throw new IllegalArgumentException("unexpected end tag of: " + parser.getName());
                }
            } else if (eventType == XmlPullParser.TEXT) {
                text = parser.getText();
            } else {
                throw new IllegalArgumentException("unexpected eventType: " + eventType);
            }
        }

        return new AbstractHttpOverXmpp.Base64(text);
    }

    private AbstractHttpOverXmpp.ChunkedBase64 parseChunkedBase64(XmlPullParser parser) throws Exception {
        String streamId = parser.getAttributeValue("", ATTRIBUTE_STREAM_ID);
        AbstractHttpOverXmpp.ChunkedBase64 child = new AbstractHttpOverXmpp.ChunkedBase64(streamId);
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(ELEMENT_CHUNKED_BASE_64)) {
                    done = true;
                } else {
                    throw new IllegalArgumentException("unexpected end tag: " + parser.getName());
                }
            } else {
                throw new IllegalArgumentException("unexpected event type: " + eventType);
            }
        }
        return child;
    }

    private AbstractHttpOverXmpp.Ibb parseIbb(XmlPullParser parser) throws Exception {
        String sid = parser.getAttributeValue("", ATTRIBUTE_SID);
        AbstractHttpOverXmpp.Ibb child = new AbstractHttpOverXmpp.Ibb(sid);
        boolean done = false;

        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(ELEMENT_IBB)) {
                    done = true;
                } else {
                    throw new IllegalArgumentException("unexpected end tag: " + parser.getName());
                }
            } else {
                throw new IllegalArgumentException("unexpected event type: " + eventType);
            }
        }
        return child;
    }
}
