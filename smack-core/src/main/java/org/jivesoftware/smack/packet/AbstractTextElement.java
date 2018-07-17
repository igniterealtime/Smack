/**
 *
 * Copyright © 2017 Florian Schmaus
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

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class AbstractTextElement implements ExtensionElement {

    public static final String ELEMENT = "text";

    private final String text;
    private final String lang;

    protected AbstractTextElement(String text, String lang) {
        this.text = StringUtils.requireNotNullNorEmpty(text, "Text must not be null nor empty");
        this.lang = lang;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optXmlLangAttribute(lang);
        xml.rightAngleBracket();
        xml.escape(text);
        xml.closeElement(this);
        return xml;
    }

    public final String getText() {
        return text;
    }

    public final String getLang() {
        return lang;
    }
}
