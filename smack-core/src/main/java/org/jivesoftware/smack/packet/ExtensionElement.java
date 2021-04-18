/**
 *
 * Copyright 2003-2007 Jive Software, 2018-2021 Florian Schmaus.
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
 * Interface to represent XMPP extension elements. Unlike {@link XmlElement}, every non-abstract class that implements
 * {@link ExtensionElement} must have a static final QNAME member of the type {@link javax.xml.namespace.QName}. This
 * allows type-safe functions like {@link StanzaView#getExtension(Class)}. Hence this is a marker interface.
 * <p>
 * Use this class when implementing new extension elements when possible. This means that every instance of your
 * implemented class must represent an XML element of the same qualified name.
 * </p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6120#section-8.4">RFC 6120 § 8.4 Extended Content</a>
 */
public interface ExtensionElement extends XmlElement {

}
