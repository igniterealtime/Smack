/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.message_correct;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * An Extension that implements XEP-0308: Last Message Correction
 * 
 * This extension is expected to be added to message stanzas. Please refer to
 * the XEP for more implementation guidelines.
 * 
 * @author Fernando Ramirez, f.e.ramirez94@gmail.com
 * @see <a href="http://xmpp.org/extensions/xep-0308.html">XEP-0308:&nbsp;Last&
 *      nbsp;Message&nbsp;Correction</a>
 */
public class MessageCorrectExtension implements ExtensionElement {

    /**
     * The XML element name of a 'message correct' extension.
     */
    public static final String ELEMENT = "replace";

    /**
     * The namespace that qualifies the XML element of a 'message correct'
     * extension.
     */
    public static final String NAMESPACE = "urn:xmpp:message-correct:0";

    /**
     * The id tag of a 'message correct' extension.
     */
    public static final String ID_TAG = "id";

    /**
     * The id of the message to correct.
     */
    private String idInitialMessage;

    public MessageCorrectExtension(String idInitialMessage) {
        this.setIdInitialMessage(idInitialMessage);
    }

    public String getIdInitialMessage() {
        return idInitialMessage;
    }

    public void setIdInitialMessage(String idInitialMessage) {
        this.idInitialMessage = idInitialMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#getElementName()
     */
    @Override
    public String getElementName() {
        return ELEMENT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#toXML()
     */
    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ID_TAG, getIdInitialMessage());
        xml.closeEmptyElement();
        return xml;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#getNamespace()
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public static MessageCorrectExtension from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

}
