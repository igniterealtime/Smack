/**
 *
 * Copyright 2018 Paul Schaub.
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

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * BodyElement that should only be used if the body is used as a child element of an element which is not a message.
 * The difference of this class to {@link Message.Body} is, that a BodyElement when serialized into xml, will
 * annotate the namespace "jabber:client".
 */
public class BodyElement extends Message.Body {

    public BodyElement(String language, String message) {
        super(language, message);
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(getElementName())
                .xmlnsAttribute(getNamespace())
                .xmllangAttribute(getLanguage())
                .rightAngleBracket();
        xml.escape(getMessage());
        xml.closeElement(getElementName());
        return xml;
    }
}
