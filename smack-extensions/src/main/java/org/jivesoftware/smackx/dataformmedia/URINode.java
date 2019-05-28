/**
 *
 * Copyright 2019 Aditya Borikar
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
package org.jivesoftware.smackx.dataformmedia;

import java.net.URI;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class URINode implements NamedElement{

    private final URI uri;
    private final String ELEMENT = "uri";
    private final String type_subtype;

    public URINode(String type, URI uri) {
        if (type == null || uri == null) {
            throw new IllegalArgumentException("Type and URI should not be null.");
        }
        this.type_subtype = "'" + type + "'";
        this.uri = uri;
    }

    public URINode(String type, String subType, URI uri) {
        this.type_subtype = "'" + type + "; " + subType + "'";
        this.uri = uri;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.append(" type=" + type_subtype);
        /*
         * xml.attribute("type",type_subtype);
         */
            xml.rightAngleBracket();
            xml.optAppend(uri.toString());
            xml.closeElement(this);
            return xml;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }
}
