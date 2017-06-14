/**
 *
 * Copyright 2016-2017 Fernando Ramirez
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
package org.jivesoftware.smackx.blocking.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.blocking.BlockingCommandManager;

/**
 * Blocked error extension class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public class BlockedErrorExtension implements ExtensionElement {

    public static final String ELEMENT = "blocked";
    public static final String NAMESPACE = BlockingCommandManager.NAMESPACE + ":errors";

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.closeEmptyElement();
        return xml;
    }

    public static BlockedErrorExtension from(Message message) {
        XMPPError error = message.getError();
        if (error == null) {
            return null;
        }
        return error.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * Check if a message contains a BlockedErrorExtension, which means that a
     * message was blocked because the JID blocked the sender, and that was
     * reflected back as an error message.
     * 
     * @param message
     * @return true if the message contains a BlockedErrorExtension, false if
     *         not
     */
    public static boolean isInside(Message message) {
        return from(message) != null;
    }

}
