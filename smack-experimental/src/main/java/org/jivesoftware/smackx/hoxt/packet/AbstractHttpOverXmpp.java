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
import org.jivesoftware.smack.util.Objects;
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

    private final HeadersExtension headers;
    private final Data data;

    private final String version;

    protected AbstractHttpOverXmpp(String element, Builder<?, ?> builder) {
        super(element, NAMESPACE);
        this.headers = builder.headers;
        this.data = builder.data;
        this.version = Objects.requireNonNull(builder.version, "version must not be null");
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        IQChildElementXmlStringBuilder builder = getIQHoxtChildElementBuilder(xml);
        builder.optAppend(headers);
        builder.optAppend(data);
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
     * Returns Headers element.
     *
     * @return Headers element
     */
    public HeadersExtension getHeaders() {
        return headers;
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
     * A builder for XMPP connection configurations.
     * <p>
     * See ConnectionConfiguration Buidler for more details.
     * </p>
     *
     * @param <B> the builder type parameter.
     * @param <C> the resulting HttpOverXmpp IQ
     */
    public static abstract class Builder<B extends Builder<B, C>, C extends AbstractHttpOverXmpp> {

        private HeadersExtension headers;
        private Data data;

        private String version = "1.1";

        /**
         * Sets Data element.
         *
         * @param data Headers element
         *
         * @return the builder
         */
        public B setData(Data data) {
            this.data = data;
            return getThis();
        }

        /**
         * Sets Headers element.
         *
         * @param headers Headers element
         * 
         * @return the builder
         */
        public B setHeaders(HeadersExtension headers) {
            this.headers = headers;
            return getThis();
        }

        /**
         * Sets version attribute.
         *
         * @param version version attribute
         *
         * @return the builder
         */
        public B setVersion(String version) {
            this.version = version;
            return getThis();
        }

        public abstract C build();

        protected abstract B getThis();
    }

    /**
     * Representation of Data element.
     * <p>
     * This class is immutable.
     */
    public static class Data implements NamedElement {

        public static final String ELEMENT = "data";

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
        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            xml.element(child);
            xml.closeElement(this);
            return xml;
        }

        /**
         * Returns element nested by Data.
         *
         * @return element nested by Data
         */
        public NamedElement getChild() {
            return child;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * Representation of Text element.
     * <p>
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
     * Representation of Base64 element.
     * <p>
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
     * Representation of Xml element.
     * <p>
     * This class is immutable.
     */
    public static class Xml implements NamedElement {

        public static final String ELEMENT = "xml";

        private final String text;

        /**
         * Creates this element.builder.toString().
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
     * Representation of ChunkedBase64 element.
     * <p>
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
     * Representation of Ibb element.
     * <p>
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
