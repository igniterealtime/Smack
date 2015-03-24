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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Mechanisms implements ExtensionElement {

    public static final String ELEMENT = "mechanisms";
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-sasl";

    public final List<String> mechanisms = new LinkedList<String>();

    public Mechanisms(String mechanism) {
        mechanisms.add(mechanism);
    }

    public Mechanisms(Collection<String> mechanisms) {
        this.mechanisms.addAll(mechanisms);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public List<String> getMechanisms() {
        return Collections.unmodifiableList(mechanisms);
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        for (String mechanism : mechanisms) {
            xml.element("mechanism", mechanism);
        }
        xml.closeElement(this);
        return xml;
    }

}
