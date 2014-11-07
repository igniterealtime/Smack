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
package org.jivesoftware.smackx.hoxt.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;

/**
 * Abstract parent for Req and Resp IQ packets.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public abstract class AbstractHttpOverXmpp extends IQ {

    public static final String NAMESPACE = "urn:xmpp:http";

    protected AbstractHttpOverXmpp(String element) {
        super(element, NAMESPACE);
    }

    private HeadersExtension headers;
    private Data data;

    protected String version;

    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        IQChildElementXmlStringBuilder builder = getIQHoxtChildElementBuilder(xml);
        builder.append(headers.toXML());
        builder.append(data.toXML());

        return builder;
    }

    /**
     * Returns start tag.
     *
     * @return start tag
     */
    protected abstract IQChildElementXmlStringBuilder getIQHoxtChildElementBuilder(IQChildElementXmlStringBuilder xml);

    /**
     * Returns version attribute.
     *
     * @return version attribute
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version attribute.
     *
     * @param version version attribute
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns Headers element.
     *
     * @return Headers element
     */
    public HeadersExtension getHeaders() {
        return headers;
    }

    /**
     * Sets Headers element.
     *
     * @param headers Headers element
     */
    public void setHeaders(HeadersExtension headers) {
        this.headers = headers;
    }

    /**
     * Returns Data element.
     *
     * @return Data element
     */
    public Data getData() {
        return data;
    }

    /**
     * Sets Data element.
     *
     * @param data Headers element
     */
    public void setData(Data data) {
        this.data = data;
    }

    /**
     * Representation of Data element.<p>
     * This class is immutable.
     */
    public static class Data {

        private final NamedElement child;

        /**
         * Creates Data element.
         *
         * @param child element nested by Data
         */
        public Data(NamedElement child) {
            this.child = child;
        }

        /**
         * Returns string containing xml representation of this object.
         *
         * @return xml representation of this object
         */
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<data>");
            builder.append(child.toXML());
            builder.append("</data>");
            return builder.toString();
        }

        /**
         * Returns element nested by Data.
         *
         * @return element nested by Data
         */
        public NamedElement getChild() {
            return child;
        }
    }

    /**
     * Representation of Text element.<p>
     * This class is immutable.
     */
    public static class Text implements NamedElement {

        public static final String ELEMENT = "text";

        private final String text;

        /**
         * Creates this element.
         *
         * @param text value of text
         */
        public Text(String text) {
            this.text = text;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            xml.optAppend(text);
            xml.closeElement(this);
            return xml;
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Representation of Base64 element.<p>
     * This class is immutable.
     */
    public static class Base64 implements NamedElement {

        public static final String ELEMENT = "base64";

        private final String text;

        /**
         * Creates this element.
         *
         * @param text value of text
         */
        public Base64(String text) {
            this.text = text;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            xml.optAppend(text);
            xml.closeElement(this);
            return xml;
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Representation of Xml element.<p>
     * This class is immutable.
     */
    public static class Xml implements NamedElement {

        public static final String ELEMENT = "xml";

        private final String text;

        /**
         * Creates this element.builder.toString();
         *
         * @param text value of text
         */
        public Xml(String text) {
            this.text = text;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            xml.optAppend(text);
            xml.closeElement(this);
            return xml;
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Representation of ChunkedBase64 element.<p>
     * This class is immutable.
     */
    public static class ChunkedBase64 implements NamedElement {

        public static final String ELEMENT = "chunkedBase64";

        private final String streamId;

        /**
         * Creates ChunkedBase86 element.
         *
         * @param streamId streamId attribute
         */
        public ChunkedBase64(String streamId) {
            this.streamId = streamId;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("streamId", streamId);
            xml.closeEmptyElement();
            return xml;
        }

        /**
         * Returns streamId attribute.
         *
         * @return streamId attribute
         */
        public String getStreamId() {
            return streamId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Representation of Ibb element.<p>
     * This class is immutable.
     */
    public static class Ibb implements NamedElement {

        public static final String ELEMENT = "ibb";

        private final String sid;

        /**
         * Creates Ibb element.
         *
         * @param sid sid attribute
         */
        public Ibb(String sid) {
            this.sid = sid;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("sid", sid);
            xml.closeEmptyElement();
            return xml;
        }

        /**
         * Returns sid attribute.
         *
         * @return sid attribute
         */
        public String getSid() {
            return sid;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
