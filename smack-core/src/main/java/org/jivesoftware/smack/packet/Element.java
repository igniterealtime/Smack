/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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
 * Interface to represent a XML element. This is similar to {@link ExtensionElement}, but does not
 * carry a namespace and is usually included as child element of an stanza extension.
 */
public interface Element {

    CharSequence toXML(XmlEnvironment xmlEnvironment);

    /**
     * Returns the XML representation of this Element. This method takes an optional argument for the enclosing
     * namespace which may be null or the empty String if the value is not known.
     *
     * @param enclosingNamespace the enclosing namespace or {@code null}.
     * @return the stanza extension as XML.
     */
    default CharSequence toXML(String enclosingNamespace) {
        XmlEnvironment xmlEnvironment = new XmlEnvironment(enclosingNamespace);
        return toXML(xmlEnvironment);
    }

    default CharSequence toXML() {
        return toXML(XmlEnvironment.EMPTY);
    }
}
