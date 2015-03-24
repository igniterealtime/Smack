/**
 *
 * Copyright 2003-2007 Jive Software.
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

/**
 * Interface to represent extension elements.
 * <p>
 * An extension element is an XML subdocument
 * with a root element name and namespace. Extension elements are used to provide
 * extended functionality beyond what is in the base XMPP specification. Examples of
 * extensions elements include message events, message properties, and extra presence data.
 * IQ stanzas have limited support for extension elements.
 * <p>
 * This class is used primarily for extended content in XMPP Stanzas, to act as so called "extension elements". For more
 * information see <a href="https://tools.ietf.org/html/rfc6120#section-8.4">RFC 6120 § 8.4 Extended Content</a>.
 * </p>
 *
 * @see DefaultExtensionElement
 * @see org.jivesoftware.smack.provider.ExtensionElementProvider
 * @author Matt Tucker
 */
public interface ExtensionElement extends NamedElement {

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace();

}
