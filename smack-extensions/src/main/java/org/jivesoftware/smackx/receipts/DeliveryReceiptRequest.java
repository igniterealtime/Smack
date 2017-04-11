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

import java.io.IOException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.id.StanzaIdUtil;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Represents a <b>message delivery receipt request</b> entry as specified by
 * <a href="http://xmpp.org/extensions/xep-0184.html">Message Delivery Receipts</a>.
 *
 * @author Georg Lukas
 */
public class DeliveryReceiptRequest implements ExtensionElement
{
    public static final String ELEMENT = "request";

    @Override
    public String getElementName()
    {
        return ELEMENT;
    }

    @Override
    public String getNamespace()
    {
        return DeliveryReceipt.NAMESPACE;
    }

    @Override
    public String toXML()
    {
        return "<request xmlns='" + DeliveryReceipt.NAMESPACE + "'/>";
    }

    /**
     * Get the {@link DeliveryReceiptRequest} extension of the packet, if any.
     *
     * @param p the packet
     * @return the {@link DeliveryReceiptRequest} extension or {@code null}
     * @deprecated use {@link #from(Stanza)} instead
     */
    @Deprecated
    public static DeliveryReceiptRequest getFrom(Stanza p) {
        return from(p);
    }

    /**
     * Get the {@link DeliveryReceiptRequest} extension of the packet, if any.
     *
     * @param packet the packet
     * @return the {@link DeliveryReceiptRequest} extension or {@code null}
     */
    public static DeliveryReceiptRequest from(Stanza packet) {
        return packet.getExtension(ELEMENT, DeliveryReceipt.NAMESPACE);
    }

    /**
     * Add a delivery receipt request to an outgoing packet.
     *
     * Only message packets may contain receipt requests as of XEP-0184,
     * therefore only allow Message as the parameter type.
     *
     * @param message Message object to add a request to
     * @return the Message ID which will be used as receipt ID
     */
    public static String addTo(Message message) {
        if (message.getStanzaId() == null) {
            message.setStanzaId(StanzaIdUtil.newStanzaId());
        }
        message.addExtension(new DeliveryReceiptRequest());
        return message.getStanzaId();
    }

    /**
     * This Provider parses and returns DeliveryReceiptRequest packets.
     */
    public static class Provider extends ExtensionElementProvider<DeliveryReceiptRequest> {
        @Override
        public DeliveryReceiptRequest parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException {
            return new DeliveryReceiptRequest();
        }
    }
}
