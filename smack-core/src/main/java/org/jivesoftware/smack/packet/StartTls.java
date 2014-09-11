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

import org.jivesoftware.smack.util.XmlStringBuilder;

public class StartTls extends FullStreamElement {

    public static final String ELEMENT = "starttls";
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-tls";

    private final boolean required;

    public StartTls() {
        this(false);
    }

    public StartTls(boolean required) {
        this.required = required;
    }

    public boolean required() {
        return required;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.condEmptyElement(required, "required");
        xml.closeElement(this);
        return xml;
    }

}
