/**
 *
 * Copyright © 2014 Florian Schmaus
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

import org.jivesoftware.smack.packet.ExtensionElement;

public class PacketUtil {

    /**
     * Get a extension element from a collection
     *
     * @param collection
     * @param element
     * @param namespace
     * @return the extension element
     * @deprecated use {@link #extensionElementFrom(Collection, String, String)} instead
     */
    @Deprecated
    public static <PE extends ExtensionElement> PE packetExtensionfromCollection(
                    Collection<ExtensionElement> collection, String element,
                    String namespace) {
        return extensionElementFrom(collection, element, namespace);
    }

    /**
     * Get a extension element from a collection
     *
     * @param collection
     * @param element
     * @param namespace
     * @return the extension element
     */
    @SuppressWarnings("unchecked")
    public static <PE extends ExtensionElement> PE extensionElementFrom(Collection<ExtensionElement> collection,
                    String element, String namespace) {
        for (ExtensionElement packetExtension : collection) {
            if ((element == null || packetExtension.getElementName().equals(
                            element))
                            && packetExtension.getNamespace().equals(namespace)) {
                return (PE) packetExtension;
            }
        }
        return null;
    }
}
