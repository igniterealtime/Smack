/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.bind2.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Bind2Elements {

    public static final String NAMESPACE = "urn:xmpp:bind:0";

    /*
     * The same <bind/> element is used in two different contexts (sasl2 stream feature inline and sasl2 authenticate) with different requirements (a tragedy specified in cold blood by dwd).
     */
    public static class Bind implements ExtensionElement {
        public static final String ELEMENT = "bind";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final Set<String> inlineFeatures;
        private final String tag;
        private final List<XmlElement> extensionElements;

        public Bind(Set<String> inlineFeatures) {
            this(inlineFeatures, null, null);
        }

        public Bind(String tag, List<? extends XmlElement> extensionElements) {
            this(null, tag, extensionElements);
        }

        public Bind(Set<String> inlineFeatures, String tag, List<? extends XmlElement> extensionElements) {
            this.inlineFeatures = inlineFeatures == null ? Collections.emptySet() : Collections.unmodifiableSet(inlineFeatures);
            this.tag = tag;
            this.extensionElements = extensionElements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extensionElements));

            if (!this.inlineFeatures.isEmpty()) {
                if (tag != null)
                    throw new IllegalArgumentException("Can't have tag and inline features set");

                if (!this.extensionElements.isEmpty())
                    throw new IllegalArgumentException("Can't have extension elements and inline features set");
            }
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public Set<String> getInlineFeatures() {
            return inlineFeatures;
        }

        public String getTag() {
            return tag;
        }

        public List<XmlElement> getExtensionElements() {
            return extensionElements;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);

            if (inlineFeatures.isEmpty() && tag == null && extensionElements.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }

            xml.rightAngleBracket();

            if (!inlineFeatures.isEmpty()) {
                xml.openElement("inline");
                for (String feature : inlineFeatures) {
                    xml.halfOpenElement("feature").attribute("var", feature).closeEmptyElement();
                }
                xml.closeElement("inline");
            }

            xml.optElement("tag", tag);
            xml.append(extensionElements);

            xml.closeElement(this);
            return xml;
        }
    }

    public static class Bound implements ExtensionElement {
        public static final String ELEMENT = "bound";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public final XmlElement mamMetadata;

        public Bound(XmlElement mamMetadata) {
            this.mamMetadata = mamMetadata;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public XmlElement getMamMetadata() {
            return mamMetadata;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);

            if (mamMetadata == null) {
                xml.closeEmptyElement();
                return xml;
            }

            xml.rightAngleBracket();
            xml.append(mamMetadata);
            xml.closeElement(this);
            return xml;
        }
    }
}
