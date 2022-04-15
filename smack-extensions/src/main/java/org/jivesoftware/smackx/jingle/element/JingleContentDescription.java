/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.jingle.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Jingle content description.
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public abstract class JingleContentDescription implements ExtensionElement {

    public static final String ELEMENT = "description";

    private final List<ExtensionElement> payloads;

    protected JingleContentDescription(List<? extends ExtensionElement> payloads) {
        if (payloads != null) {
            this.payloads = Collections.unmodifiableList(payloads);
        }
        else {
            this.payloads = Collections.emptyList();
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public List<ExtensionElement> getJingleContentDescriptionChildren() {
        return payloads;
    }

    protected void addExtraAttributes(XmlStringBuilder xml) {

    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        addExtraAttributes(xml);
        xml.rightAngleBracket();

        xml.append(payloads);

        xml.closeElement(this);
        return xml;
    }

}
