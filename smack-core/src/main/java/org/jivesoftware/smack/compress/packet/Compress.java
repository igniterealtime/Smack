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
package org.jivesoftware.smack.compress.packet;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.FullStreamElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Compress extends FullStreamElement {

    public static final String ELEMENT = "compress";
    public static final String NAMESPACE = "http://jabber.org/protocol/compress";

    public final String method;

    public Compress(String method) {
        this.method = method;
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
        xml.element("method", method);
        xml.closeElement(this);
        return xml;
    }

    public static class Feature implements ExtensionElement {
        public static final String ELEMENT = "compression";

        public final List<String> methods;

        public Feature(List<String> methods) {
            this.methods = methods;
        }

        public List<String> getMethods() {
            return Collections.unmodifiableList(methods);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            for (String method : methods) {
                xml.element("method", method);
            }
            xml.closeElement(this);
            return xml;
        }
    }
}
