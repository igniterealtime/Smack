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

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ListElement implements MarkupElement.MarkupChildElement {

    public static final String ELEMENT = "list";
    public static final String ELEM_LI = "li";

    private final int start, end;
    private final List<ListEntryElement> entries;

    /**
     * Create a new List element.
     *
     * @param start start index of the list
     * @param end end index of the list
     * @param entries list entries
     */
    public ListElement(int start, int end, List<ListEntryElement> entries) {
        this.start = start;
        this.end = end;
        this.entries = Collections.unmodifiableList(entries);
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    /**
     * Return a list of all list entries.
     *
     * @return entries
     */
    public List<ListEntryElement> getEntries() {
        return entries;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(this);
        xml.attribute(ATTR_START, getStart());
        xml.attribute(ATTR_END, getEnd());
        xml.rightAngleBracket();

        for (ListEntryElement li : getEntries()) {
            xml.append(li.toXML());
        }

        xml.closeElement(this);
        return xml;
    }

    public static class ListEntryElement implements NamedElement {

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
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.attribute(ATTR_START, getStart());
            xml.closeEmptyElement();
            return xml;
        }
    }
}
