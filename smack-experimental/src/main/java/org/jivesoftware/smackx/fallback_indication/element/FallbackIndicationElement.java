/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.fallback_indication.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class FallbackIndicationElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:fallback:0";
    public static final String ELEMENT = "fallback";

    public static final FallbackIndicationElement INSTANCE = new FallbackIndicationElement();

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).closeEmptyElement();
    }

    public static boolean hasFallbackIndication(Message message) {
        return message.hasExtension(ELEMENT, NAMESPACE);
    }

    public static FallbackIndicationElement fromMessage(Message message) {
        return message.getExtension(FallbackIndicationElement.class);
    }
}
