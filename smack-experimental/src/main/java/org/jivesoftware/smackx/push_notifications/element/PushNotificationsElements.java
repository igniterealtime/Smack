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
package org.jivesoftware.smackx.push_notifications.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jxmpp.jid.Jid;

/**
 * Push Notifications elements.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0357.html">XEP-0357: Push
 *      Notifications</a>
 * @author Fernando Ramirez
 * 
 */
public class PushNotificationsElements {

    public static final String NAMESPACE = "urn:xmpp:push:0";

    public static class RemoteDisablingExtension implements ExtensionElement {

        public static final String NAMESPACE = PubSub.NAMESPACE;
        public static final String ELEMENT = PubSub.ELEMENT;

        private final String node;
        private final Jid userJid;

        public RemoteDisablingExtension(String node, Jid userJid) {
            this.node = node;
            this.userJid = userJid;
        }

        @Override
        public String getElementName() {
            return PubSub.ELEMENT;
        }

        @Override
        public String getNamespace() {
            return PubSub.NAMESPACE;
        }

        /**
         * Get the node.
         * 
         * @return the node
         */
        public String getNode() {
            return node;
        }

        /**
         * Get the user JID.
         * 
         * @return the user JID
         */
        public Jid getUserJid() {
            return userJid;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);

            xml.attribute("node", node);
            xml.rightAngleBracket();

            xml.halfOpenElement("affiliation");
            xml.attribute("jid", userJid);
            xml.attribute("affiliation", "none");
            xml.closeEmptyElement();

            xml.closeElement(this);
            return xml;
        }

        public static RemoteDisablingExtension from(Message message) {
            return message.getExtension(ELEMENT, NAMESPACE);
        }

    }

}
