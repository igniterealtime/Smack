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
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ListElement extends MarkupElement.NonEmptyChildElement {

    public static final String ELEMENT = "list";
    public static final String ELEM_LI = "li";

    private final List<ListEntryElement> entries;

    /**
     * Create a new List element.
     *
     * @param start start index of the list
     * @param end end index of the list
     * @param entries list entries
     */
    public ListElement(int start, int end, List<ListEntryElement> entries) {
        super(start, end);
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * Return a list of all list entries.
     *
     * @return entries TODO javadoc me please
     */
    public List<ListEntryElement> getEntries() {
        return entries;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public void appendInnerXml(XmlStringBuilder xml) {
        xml.append(getEntries());
    }

    public static class ListEntryElement implements ExtensionElement {

        private final int start;

        /**
         * Create a new ListEntry element.
         *
         * @param start start index
         */
        public ListEntryElement(int start) {
            this.start = start;
        }

        /**
         * Return the start index of this entry.
         * @return start index
         */
        public int getStart() {
            return start;
        }

        @Override
        public String getElementName() {
            return ELEM_LI;
        }

        @Override
        public String getNamespace() {
            return MarkupElement.NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
            xml.attribute(MarkupElement.MarkupChildElement.ATTR_START, getStart());
            xml.closeEmptyElement();
            return xml;
        }
    }
}
