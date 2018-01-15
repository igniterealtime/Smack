/**
 *
 * Copyright © 2018 Paul Schaub
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
package org.jivesoftware.smackx.message_markup.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class MarkupElement implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:markup:0";
    public static final String ELEMENT = "markup";

    private final List<MarkupChildElement> childElements;

    /**
     * Create a new MarkupElement.
     *
     * @param childElements child elements.
     */
    public MarkupElement(List<MarkupChildElement> childElements) {
        this.childElements = Collections.unmodifiableList(childElements);
    }

    /**
     * Return a list of all child elements.
     * @return children
     */
    public List<MarkupChildElement> getChildElements() {
        return childElements;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this).rightAngleBracket();

        for (MarkupChildElement child : getChildElements()) {
            xml.append(child.toXML());
        }

        xml.closeElement(this);
        return xml;
    }

    /**
     * Interface for child elements.
     */
    public interface MarkupChildElement extends NamedElement {

        /**
         * Return the start index of this element.
         *
         * @return start index
         */
        int getStart();

        /**
         * Return the end index of this element.
         *
         * @return end index
         */
        int getEnd();
    }

    /**
     * Interface for block level child elements.
     */
    public interface BlockLevelMarkupElement extends MarkupChildElement {

    }
}
