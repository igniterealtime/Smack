/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.shim.packet;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Extension representing a list of headers as specified in <a href="http://xmpp.org/extensions/xep-0131">Stanza Headers and Internet Metadata (SHIM)</a>.
 * 
 * @see Header
 * 
 * @author Robin Collier
 */
public class HeadersExtension implements ExtensionElement {
    public static final String ELEMENT = "headers";
    public static final String NAMESPACE = "http://jabber.org/protocol/shim";

    private final List<Header> headers;

    public HeadersExtension(List<Header> headerList) {
        if (headerList != null) {
            headers = Collections.unmodifiableList(headerList);
        } else {
            headers = Collections.emptyList();
        }
    }

    public List<Header> getHeaders() {
        return headers;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.append(headers);
        xml.closeElement(this);
        return xml;
    }

    /**
     * Return the SHIM headers extension of this stanza or null if there is none.
     *
     * @param packet
     * @return the headers extension or null.
     */
    public static HeadersExtension from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }
}
