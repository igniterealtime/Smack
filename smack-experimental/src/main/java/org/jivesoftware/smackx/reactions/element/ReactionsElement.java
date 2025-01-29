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

import java.util.Collections;
import java.util.List;

/**
 * Represents the reactions element in an XMPP message. This class is used to manage and serialize
 * the list of reactions associated with a message, including the reactions' emojis and their identifiers.
 * It is used as an extension element in XMPP messages to allow reactions to be sent and received.
 *
 * @see Reaction
 * @see Message
 * @see ExtensionElement
 */
public class ReactionsElement implements ExtensionElement {
    public static final String ELEMENT = "reactions";
    public static final String NAMESPACE = "urn:xmpp:reactions:0";

    private final List<Reaction> reactions;
    private final String id;

    /**
     * Constructs a new ReactionsElement with a list of reactions and an identifier.
     *
     * @param reactions A list of reactions associated with a message.
     * @param id The ID of the message being reacted to.
     */
    public ReactionsElement(List<Reaction> reactions, String id) {
        this.reactions = Collections.unmodifiableList(reactions);
        this.id = id;
    }

    /**
     * Retrieves the list of reactions in this element.
     *
     * @return The list of reactions.
     */
    public List<Reaction> getReactions() {
        return reactions;
    }

    /**
     * Retrieves the ID of the original message being reacted to.
     *
     * @return The ID of the original message.
     */
    public String getId() {
        return id;
    }


    /**
     * Returns the namespace for this extension element.
     *
     * @return The namespace of the reactions element.
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Returns the name of the XML element associated with this extension.
     *
     * @return The element name for this extension, which is "reactions".
     */
    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /**
     * Converts this ReactionsElement into an XML representation that can be included in an XMPP message.
     *
     * @param xmlEnvironment The XML environment for serialization.
     * @return The XML string builder with the XML representation of the element.
     */
    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("id", id);
        for (Reaction reaction : reactions) {
            xml.append(reaction.toXML(xmlEnvironment));
        }
        xml.closeElement(this);
        return xml;
    }

    /**
     * Retrieves the ReactionsElement from an XMPP message.
     *
     * @param message The XMPP message from which the reactions element is extracted.
     * @return The ReactionsElement from the message, or {@code null} if not present.
     */
    public static ReactionsElement fromMessage(Message message){
        return message.getExtension(ReactionsElement.class);
    }
}
