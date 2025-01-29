/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a reaction in the form of an emoji to be associated with a message in XMPP. This class
 * is used to handle the individual reaction data, including the emoji and its associated message.
 * It is an extension element used in XMPP messages.
 *
 * @see ExtensionElement
 * @see Message
 */
public class Reaction implements ExtensionElement {

    public static final String ELEMENT = "reaction";
    public static final String NAMESPACE = "";

    private final String emoji;

    /**
     * Constructs a new Reaction with the specified emoji.
     *
     * @param emoji The emoji representing the reaction.
     */
    public Reaction(String emoji) {
        this.emoji = emoji;
    }

    /**
     * Retrieves the emoji associated with this reaction.
     *
     * @return The emoji as a string.
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Returns the namespace for this extension element. As the namespace is empty, it returns an empty string.
     *
     * @return The namespace of the reaction element, which is an empty string.
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Returns the name of the XML element for this reaction, which is "reaction".
     *
     * @return The name of the XML element, which is "reaction".
     */
    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /**
     * Converts this Reaction into an XML representation that can be included in an XMPP message.
     *
     * @param xmlEnvironment The XML environment for serializing the element.
     * @return The XML string builder containing the XML representation of the reaction element.
     */
    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.openElement(getElementName());
        xml.append(getEmoji());
        xml.closeElement(getElementName());
        return xml;
    }

    /**
     * Retrieves the Reaction extension from an XMPP message.
     *
     * @param message The XMPP message from which to extract the reaction.
     * @return The Reaction extension from the message, or {@code null} if not present.
     */
    public static Reaction fromMessage(Message message){
        return message.getExtension(Reaction.class);
    }
}
