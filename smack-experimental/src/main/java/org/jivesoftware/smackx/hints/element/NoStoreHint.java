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
 * A "no store" hint. Messages with this hint should not be stored in stores or archives.
 *
 * <a href="https://xmpp.org/extensions/xep-0334.html#no-store">XEP-0334 § 4.2 No store</a>
 */
public final class NoStoreHint extends MessageProcessingHint {

    public static final NoStoreHint INSTANCE = new NoStoreHint();

    public static final String ELEMENT = "no-store";

    private NoStoreHint() {
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
        return MessageProcessingHintType.no_store;
    }

    public static NoStoreHint from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

    public static boolean hasHint(Message message) {
        return from(message) != null;
    }

    public static void set(Message message) {
        message.overrideExtension(INSTANCE);
    }
}
