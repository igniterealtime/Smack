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
package org.jivesoftware.smackx.reply.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;


public class ReplyElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:reply:0";
    public static final String ELEMENT = "reply";

    private final String replyTo;
    private final String replyId;

    public ReplyElement(String replyTo, String replyId) {
        this.replyTo = replyTo;
        this.replyId = replyId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getReplyId() {
        return replyId;
    }

    @Override public String getNamespace() {
        return NAMESPACE;
    }

    @Override public String getElementName() {
        return ELEMENT;
    }

    @Override public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);

        if (replyTo != null) {
            xml.attribute("to", replyTo);
        }
        if (replyId != null) {
            xml.attribute("id", replyId);
        }

        return xml.closeEmptyElement();
    }

    public static ReplyElement fromMessage(Message message) {
        return message.getExtension(ReplyElement.class);
    }

}
