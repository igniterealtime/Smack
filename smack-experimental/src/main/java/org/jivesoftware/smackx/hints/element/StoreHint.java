/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.hints.element;

import org.jivesoftware.smack.packet.Message;

/**
 * A "store" hint. Messages with this hint should be stored in permanent stores or archives.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0334.html#sect-idm140684698220992">XEP-0334 § 4.4 Store</a>
 */
public final class StoreHint extends MessageProcessingHint {

    public static final StoreHint INSTANCE = new StoreHint();

    public static final String ELEMENT = "store";

    private StoreHint() {
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return '<' + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
    }

    @Override
    public MessageProcessingHintType getHintType() {
        return MessageProcessingHintType.store;
    }

    public static StoreHint from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

    public static boolean hasHint(Message message) {
        return from(message) != null;
    }

    public static void set(Message message) {
        message.overrideExtension(INSTANCE);
    }
}
