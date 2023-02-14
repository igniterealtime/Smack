/**
 *
 * Copyright © 2014-2021 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.util.Collection;

import org.jivesoftware.smack.packet.XmlElement;

public class PacketUtil {

    /**
     * Get a extension element from a collection.
     *
     * @param collection Collection of ExtensionElements.
     * @param element name of the targeted ExtensionElement.
     * @param namespace namespace of the targeted ExtensionElement.
     * @param <PE> Type of the ExtensionElement
     *
     * @return the extension element
     */
    @SuppressWarnings("unchecked")
    public static <PE extends XmlElement> PE extensionElementFrom(Collection<XmlElement> collection,
                    String element, String namespace) {
        for (XmlElement packetExtension : collection) {
            if ((element == null || packetExtension.getElementName().equals(
                            element))
                            && packetExtension.getNamespace().equals(namespace)) {
                return (PE) packetExtension;
            }
        }
        return null;
    }
}
