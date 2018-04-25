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

import org.jivesoftware.smack.util.XmlStringBuilder;

public class CodeBlockElement implements MarkupElement.BlockLevelMarkupElement {

    public static final String ELEMENT = "bcode";

    private final int start, end;

    /**
     * Create a new Code Block element.
     *
     * @param start start index
     * @param end end index
     */
    public CodeBlockElement(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(this);
        xml.attribute(ATTR_START, getStart());
        xml.attribute(ATTR_END, getEnd());
        xml.closeEmptyElement();
        return xml;
    }
}
