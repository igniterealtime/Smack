/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.xmlelement.element;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;

public class DataFormsXmlElement implements FormFieldChildElement {

    public static final String ELEMENT = "wrapper";

    public static final String NAMESPACE = "urn:xmpp:xml-element";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final StandardExtensionElement payload;

    public DataFormsXmlElement(StandardExtensionElement payload) {
        this.payload = payload;
    }

    @Override
    public QName getQName() {
        return QNAME;
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
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        if (payload == null) {
            return xml.closeEmptyElement();
        }

        xml.rightAngleBracket();

        xml.append(payload.toXML());

        xml.closeElement(this);
        return xml;
    }

    public static DataFormsXmlElement from(FormField formField) {
        return (DataFormsXmlElement) formField.getFormFieldChildElement(QNAME);
    }
}
