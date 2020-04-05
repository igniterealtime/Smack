/**
 *
 * Copyright © 2016 Fernando Ramirez, 2018-2020 Florian Schmaus
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

import javax.xml.namespace.QName;

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
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

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
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.closeEmptyElement();
            return xml;
        }

        public static MarkableExtension from(Message message) {
            return message.getExtension(MarkableExtension.class);
        }
    }

    protected abstract static class ChatMarkerExtensionWithId implements ExtensionElement {
        protected final String id;

        protected ChatMarkerExtensionWithId(String id) {
            this.id = StringUtils.requireNotNullNorEmpty(id, "Message ID must not be null");
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public final String getId() {
            return id;
        }

        @Override
        public final XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("id", id);
            xml.closeEmptyElement();
            return xml;
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
    public static class ReceivedExtension extends ChatMarkerExtensionWithId {

        /**
         * received element.
         */
        public static final String ELEMENT = ChatMarkersState.received.toString();
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public ReceivedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static ReceivedExtension from(Message message) {
            return message.getExtension(ReceivedExtension.class);
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
    public static class DisplayedExtension extends ChatMarkerExtensionWithId {

        /**
         * displayed element.
         */
        public static final String ELEMENT = ChatMarkersState.displayed.toString();
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public DisplayedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static DisplayedExtension from(Message message) {
            return message.getExtension(DisplayedExtension.class);
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
    public static class AcknowledgedExtension extends ChatMarkerExtensionWithId {

        /**
         * acknowledged element.
         */
        public static final String ELEMENT = ChatMarkersState.acknowledged.toString();
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public AcknowledgedExtension(String id) {
            super(id);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public static AcknowledgedExtension from(Message message) {
            return message.getExtension(AcknowledgedExtension.class);
        }
    }

}
