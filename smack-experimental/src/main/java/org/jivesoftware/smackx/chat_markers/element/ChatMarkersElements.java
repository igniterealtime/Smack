/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.chat_markers.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.chat_markers.ChatMarkersState;

/**
 * Chat Markers elements (XEP-0333).
 *
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 *      Markers</a>
 * @author Fernando Ramirez
 *
 */
public class ChatMarkersElements {

    public static final String NAMESPACE = "urn:xmpp:chat-markers:0";

    /**
     * Markable extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static final class MarkableExtension implements ExtensionElement {

        public static final MarkableExtension INSTANCE = new MarkableExtension();

        /**
         * markable element.
         */
        public static final String ELEMENT = ChatMarkersState.markable.toString();

        private MarkableExtension() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.closeEmptyElement();
            return xml;
        }

        public static MarkableExtension from(Message message) {
            return (MarkableExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    /**
     * Received extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class ReceivedExtension implements ExtensionElement {

        /**
         * received element.
         */
        public static final String ELEMENT = ChatMarkersState.received.toString();

        private final String id;

        public ReceivedExtension(String id) {
            this.id = StringUtils.requireNotNullNorEmpty(id, "Message ID must not be null");
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("id", id);
            xml.closeEmptyElement();
            return xml;
        }

        public static ReceivedExtension from(Message message) {
            return (ReceivedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    /**
     * Displayed extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class DisplayedExtension implements ExtensionElement {

        /**
         * displayed element.
         */
        public static final String ELEMENT = ChatMarkersState.displayed.toString();

        private final String id;

        public DisplayedExtension(String id) {
            this.id = StringUtils.requireNotNullNorEmpty(id, "Message ID must not be null");
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("id", id);
            xml.closeEmptyElement();
            return xml;
        }

        public static DisplayedExtension from(Message message) {
            return (DisplayedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

    /**
     * Acknowledged extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
     *      Markers</a>
     * @author Fernando Ramirez
     *
     */
    public static class AcknowledgedExtension implements ExtensionElement {

        /**
         * acknowledged element.
         */
        public static final String ELEMENT = ChatMarkersState.acknowledged.toString();

        private final String id;

        public AcknowledgedExtension(String id) {
            this.id = StringUtils.requireNotNullNorEmpty(id, "Message id must not be null");
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("id", id);
            xml.closeEmptyElement();
            return xml;
        }

        public static AcknowledgedExtension from(Message message) {
            return (AcknowledgedExtension) message.getExtension(ELEMENT, NAMESPACE);
        }
    }

}
