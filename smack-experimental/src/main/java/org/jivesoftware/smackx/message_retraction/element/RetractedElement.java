/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.message_retraction.element;

import java.util.Date;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.sid.element.OriginIdElement;

public class RetractedElement implements ExtensionElement {

    public static final String ELEMENT = "retracted";
    public static final QName QNAME = new QName(RetractElement.NAMESPACE, ELEMENT);
    public static final String ATTR_STAMP = "stamp";

    private final Date stamp;
    private final OriginIdElement originId;

    public RetractedElement(Date stamp, OriginIdElement originId) {
        this.stamp = stamp;
        this.originId = originId;
    }

    public Date getStamp() {
        return stamp;
    }

    public OriginIdElement getOriginId() {
        return originId;
    }

    @Override
    public String getNamespace() {
        return QNAME.getNamespaceURI();
    }

    @Override
    public String getElementName() {
        return QNAME.getLocalPart();
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_STAMP, getStamp())
                .rightAngleBracket()
                .append(getOriginId())
                .closeElement(this);
    }
}
