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

package org.jivesoftware.smack.packet;

/**
 * Interface to represent a XML element. This is similar to {@link ExtensionElement}, but does not
 * carry a namespace and is usually included as child element of an stanza(/packet) extension.
 */
public interface NamedElement extends Element {

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName();

}
