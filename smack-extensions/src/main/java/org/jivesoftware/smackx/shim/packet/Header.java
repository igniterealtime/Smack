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

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a <b>Header</b> entry as specified by the <a href="http://xmpp.org/extensions/xep-031.html">Stanza Headers and Internet Metadata (SHIM)</a>.

 * @author Robin Collier
 */
public class Header implements ExtensionElement {
    public static final String ELEMENT = "header";

    private final String name;
    private final String value;

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return HeadersExtension.NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        // Upcast to NamedElement since we don't want a xmlns attribute
        XmlStringBuilder xml = new XmlStringBuilder((NamedElement) this);
        xml.attribute("name", name);
        xml.rightAngleBracket();
        xml.escape(value);
        xml.closeElement(this);
        return xml;
    }
}
