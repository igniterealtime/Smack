/**
 *
 * Copyright 2003-2010 Jive Software.
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
package org.jivesoftware.smackx.attention.packet;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A PacketExtension that implements XEP-0224: Attention
 * 
 * This extension is expected to be added to message stanzas of type 'headline.'
 * Please refer to the XEP for more implementation guidelines.
 * 
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a
 *      href="http://xmpp.org/extensions/xep-0224.html">XEP-0224:&nbsp;Attention</a>
 */
public class AttentionExtension implements ExtensionElement {

    /**
     * The XML element name of an 'attention' extension.
     */
    public static final String ELEMENT_NAME = "attention";

    /**
     * The namespace that qualifies the XML element of an 'attention' extension.
     */
    public static final String NAMESPACE = "urn:xmpp:attention:0";

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#getElementName()
     */
    public String getElementName() {
        return ELEMENT_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#getNamespace()
     */
    public String getNamespace() {
        return NAMESPACE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jivesoftware.smack.packet.PacketExtension#toXML()
     */
    public String toXML() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<").append(getElementName()).append(" xmlns=\"").append(
                getNamespace()).append("\"/>");
        return sb.toString();
    }

    /**
     * A {@link ExtensionElementProvider} for the {@link AttentionExtension}. As
     * Attention elements have no state/information other than the element name
     * and namespace, this implementation simply returns new instances of
     * {@link AttentionExtension}.
     * 
     * @author Guus der Kinderen, guus.der.kinderen@gmail.com
s     */
    public static class Provider extends ExtensionElementProvider<AttentionExtension> {

        @Override
        public AttentionExtension parse(XmlPullParser parser, int initialDepth) {
            return new AttentionExtension();
        }
    }
}
