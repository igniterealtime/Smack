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
import org.jivesoftware.smackx.shim.packet.HeadersExtension;

/**
 * Abstract parent for Req and Resp IQ packets.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public abstract class AbstractHttpOverXmpp extends IQ {

    /**
     * Abstract representation of parent of Req and Resp elements.
     */
    public static abstract class AbstractBody {

        private HeadersExtension headers;
        private Data data;

        protected String version;

        /**
         * Returns string containing xml representation of this object.
         *
         * @return xml representation of this object
         */
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append(getStartTag());
            builder.append(headers.toXML());
            builder.append(data.toXML());
            builder.append(getEndTag());
            return builder.toString();
        }

        /**
         * Returns start tag.
         *
         * @return start tag
         */
        protected abstract String getStartTag();

        /**
         * Returns end tag.
         *
         * @return end tag
         */
        protected abstract String getEndTag();

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
    }

    /**
     * Representation of Data element.<p>
     * This class is immutable.
     */
    public static class Data {

        private final DataChild child;

        /**
         * Creates Data element.
         *
         * @param child element nested by Data
         */
        public Data(DataChild child) {
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
        public DataChild getChild() {
            return child;
        }
    }

    /**
     * Interface for child elements of Data element.
     */
    public static interface DataChild {

        /**
         * Returns string containing xml representation of this object.
         *
         * @return xml representation of this object
         */
        public String toXML();
    }

    /**
     * Representation of Text element.<p>
     * This class is immutable.
     */
    public static class Text implements DataChild {

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
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<text>");
            if (text != null) {
                builder.append(text);
            }
            builder.append("</text>");
            return builder.toString();
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Representation of Base64 element.<p>
     * This class is immutable.
     */
    public static class Base64 implements DataChild {

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
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<base64>");
            if (text != null) {
                builder.append(text);
            }
            builder.append("</base64>");
            return builder.toString();
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Representation of Xml element.<p>
     * This class is immutable.
     */
    public static class Xml implements DataChild {

        private final String text;

        /**
         * Creates this element.
         *
         * @param text value of text
         */
        public Xml(String text) {
            this.text = text;
        }

        @Override
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<xml>");
            if (text != null) {
                builder.append(text);
            }
            builder.append("</xml>");
            return builder.toString();
        }

        /**
         * Returns text of this element.
         *
         * @return text
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Representation of ChunkedBase64 element.<p>
     * This class is immutable.
     */
    public static class ChunkedBase64 implements DataChild {

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
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<chunkedBase64 streamId='");
            builder.append(streamId);
            builder.append("'/>");
            return builder.toString();
        }

        /**
         * Returns streamId attribute.
         *
         * @return streamId attribute
         */
        public String getStreamId() {
            return streamId;
        }
    }

    /**
     * Representation of Ibb element.<p>
     * This class is immutable.
     */
    public static class Ibb implements DataChild {

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
        public String toXML() {
            StringBuilder builder = new StringBuilder();
            builder.append("<ibb sid='");
            builder.append(sid);
            builder.append("'/>");
            return builder.toString();
        }

        /**
         * Returns sid attribute.
         *
         * @return sid attribute
         */
        public String getSid() {
            return sid;
        }
    }
}
