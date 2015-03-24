/**
 *
 * Copyright 2013-2014 the original author or authors
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
package org.jivesoftware.smackx.receipts;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a <b>message delivery receipt</b> entry as specified by
 * <a href="http://xmpp.org/extensions/xep-0184.html">Message Delivery Receipts</a>.
 *
 * @author Georg Lukas
 */
public class DeliveryReceipt implements ExtensionElement
{
    public static final String NAMESPACE = "urn:xmpp:receipts";
    public static final String ELEMENT = "received";

    /**
     * original ID of the delivered message
     */
    private final String id;

    public DeliveryReceipt(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public String getElementName()
    {
        return ELEMENT;
    }

    @Override
    public String getNamespace()
    {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML()
    {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("id", id);
        xml.closeEmptyElement();
        return xml;
    }

    /**
     * Get the {@link DeliveryReceipt} extension of the packet, if any.
     *
     * @param p the packet
     * @return the {@link DeliveryReceipt} extension or {@code null}
     * @deprecated use {@link #from(Message)} instead
     */
    @Deprecated
    public static DeliveryReceipt getFrom(Message p) {
        return from(p);
    }

    /**
     * Get the {@link DeliveryReceipt} extension of the message, if any.
     *
     * @param message the message.
     * @return the {@link DeliveryReceipt} extension or {@code null}
     */
    public static DeliveryReceipt from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * This Provider parses and returns DeliveryReceipt packets.
     */
    public static class Provider extends EmbeddedExtensionProvider<DeliveryReceipt>
    {

        @Override
        protected DeliveryReceipt createReturnExtension(String currentElement, String currentNamespace,
                Map<String, String> attributeMap, List<? extends ExtensionElement> content)
        {
            return new DeliveryReceipt(attributeMap.get("id"));
        }

    }
}
