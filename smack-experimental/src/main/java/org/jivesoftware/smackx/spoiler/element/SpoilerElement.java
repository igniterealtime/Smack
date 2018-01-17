/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.spoiler.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.spoiler.SpoilerManager;

public class SpoilerElement implements ExtensionElement {

    public static final String ELEMENT = "spoiler";
    public static final SpoilerElement EMPTY = new SpoilerElement(null, null);

    private final String hint;
    private final String language;

    /**
     * Create a new SpoilerElement with a hint about a content and a language attribute.
     *
     * @param language language of the hint.
     * @param hint hint about the content.
     */
    public SpoilerElement(String language, String hint) {
        if (language != null && !language.equals("")) {
            if (hint == null || hint.equals("")) {
                throw new IllegalArgumentException("Hint cannot be null or empty if language is not empty.");
            }
        }
        this.language = language;
        this.hint = hint;
    }

    /**
     * Return the hint text of the spoiler.
     * May be null.
     *
     * @return hint text
     */
    public String getHint() {
        return hint;
    }

    /**
     * Return the language of the hint.
     * May be null.
     *
     * @return language of hint text
     */
    public String getLanguage() {
        return language;
    }

    @Override
    public String getNamespace() {
        return SpoilerManager.NAMESPACE_0;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optXmlLangAttribute(getLanguage());
        if (getHint() == null) {
            xml.closeEmptyElement();
        } else {
            xml.rightAngleBracket();
            xml.append(getHint());
            xml.closeElement(this);
        }
        return xml;
    }
}
