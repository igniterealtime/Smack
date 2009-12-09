/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
 * Interface to represent packet extensions. A packet extension is an XML subdocument
 * with a root element name and namespace. Packet extensions are used to provide
 * extended functionality beyond what is in the base XMPP specification. Examples of
 * packet extensions include message events, message properties, and extra presence data.
 * IQ packets cannot contain packet extensions.
 *
 * @see DefaultPacketExtension
 * @see org.jivesoftware.smack.provider.PacketExtensionProvider
 * @author Matt Tucker
 */
public interface PacketExtension {

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName();

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace();

    /**
     * Returns the XML representation of the PacketExtension.
     *
     * @return the packet extension as XML.
     */
    public String toXML();
}
