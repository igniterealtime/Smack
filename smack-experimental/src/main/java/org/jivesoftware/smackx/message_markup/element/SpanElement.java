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
import java.util.Set;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class SpanElement extends MarkupElement.NonEmptyChildElement {

    public static final String ELEMENT = "span";

    private final Set<SpanStyle> styles;

    /**
     * Create a new Span element.
     *
     * @param start start index
     * @param end end index
     * @param styles list of styles that apply to this span
     */
    public SpanElement(int start, int end, Set<SpanStyle> styles) {
        super(start, end);
        this.styles = Collections.unmodifiableSet(styles);
    }

    /**
     * Return all styles of this span.
     *
     * @return styles TODO javadoc me please
     */
    public Set<SpanStyle> getStyles() {
        return styles;
    }

    public static final String emphasis = "emphasis";
    public static final String code = "code";
    public static final String deleted = "deleted";

    public enum SpanStyle {
        emphasis,
        code,
        deleted
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    protected void appendInnerXml(XmlStringBuilder xml) {
        for (SpanStyle style : getStyles()) {
            xml.emptyElement(style);
        }
    }
}
