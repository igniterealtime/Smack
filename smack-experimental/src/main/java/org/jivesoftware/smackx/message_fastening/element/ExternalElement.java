/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.message_fastening.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Child element of {@link FasteningElement}.
 * Reference to a top level element in the stanza that contains the {@link FasteningElement}.
 */
public class ExternalElement implements NamedElement {

    public static final String ELEMENT = "external";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_ELEMENT_NAMESPACE = "element-namespace";

    private final String name;
    private final String elementNamespace;

    /**
     * Create a new {@link ExternalElement} that references a top level element with the given name.
     *
     * @param name name of the top level element
     */
    public ExternalElement(String name) {
        this(name, null);
    }

    /**
     * Create a new {@link ExternalElement} that references a top level element with the given name and namespace.
     *
     * @param name name of the top level element
     * @param elementNamespace namespace of the top level element
     */
    public ExternalElement(String name, String elementNamespace) {
        this.name = name;
        this.elementNamespace = elementNamespace;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_NAME, getName());
        xml.optAttribute(ATTR_ELEMENT_NAMESPACE, getElementNamespace());
        return xml.closeEmptyElement();
    }

    /**
     * Name of the referenced top level element, eg. 'body'.
     * @return element name
     */
    public String getName() {
        return name;
    }

    /**
     * Namespace of the referenced top level element, eg. 'urn:example:lik'.
     * @return element namespace
     */
    public String getElementNamespace() {
        return elementNamespace;
    }
}
