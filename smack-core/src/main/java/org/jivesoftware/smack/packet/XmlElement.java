/**
 *
 * Copyright 2018-2021 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import javax.xml.namespace.QName;

/**
 * Interface to represent XML elements. Every XML element in XMPP has a qualified XML name ({@link QName}). This name
 * can be obtained via {@link #getQName()}.
 * <p>
 * XMPP uses "extension elements", i.e. XML elements, to provide extended functionality beyond what is in the base XMPP
 * specification. Examples of extensions elements include message events, message properties, and extra presence data.
 * IQ stanzas have limited support for extension elements. See {@link ExtensionElement} for more information about XMPP
 * extension elements.
 * </p>
 * <p>
 * It is recommend to use {@link ExtensionElement} over this class when creating new extension elements.
 * </p>
 *
 * @see org.jivesoftware.smack.provider.ExtensionElementProvider
 * @since 4.5
 */
public interface XmlElement extends NamedElement, XmlLangElement {

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    String getNamespace();

    default QName getQName() {
        String namespaceURI = getNamespace();
        String localPart = getElementName();
        return new QName(namespaceURI, localPart);
    }

    @Override
    default String getLanguage() {
        return null;
    }
}
