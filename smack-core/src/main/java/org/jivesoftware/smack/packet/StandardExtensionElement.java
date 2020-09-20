/**
 *
 * Copyright 2015-2020 Florian Schmaus.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * An {@link ExtensionElement} modeling the often required and used XML features when using XMPP. It
 * is therefore suitable for most use cases. Use
 * {@link StandardExtensionElement#builder(String, String)} to build these elements.
 * <p>
 * Note the this is only meant as catch-all if no particular extension element provider is
 * registered. Protocol implementations should prefer to model their own extension elements tailored
 * to their use cases.
 * </p>
 *
 * @since 4.2
 * @author Florian Schmaus
 */
public final class StandardExtensionElement implements ExtensionElement {

    private final String name;
    private final String namespace;
    private final Map<String, String> attributes;
    private final String text;
    private final MultiMap<QName, StandardExtensionElement> elements;

    private XmlStringBuilder xmlCache;

    /**
     * Constructs a new extension element with the given name and namespace and nothing else.
     * <p>
     * This is meant to construct extension elements used as simple flags in Stanzas.
     * <p>
     *
     * @param name the name of the extension element.
     * @param namespace the namespace of the extension element.
     */
    public StandardExtensionElement(String name, String namespace) {
        this(name, namespace, null, null, null);
    }

    private StandardExtensionElement(String name, String namespace, Map<String, String> attributes, String text,
                    MultiMap<QName, StandardExtensionElement> elements) {
        this.name = StringUtils.requireNotNullNorEmpty(name, "Name must not be null nor empty");
        this.namespace = StringUtils.requireNotNullNorEmpty(namespace, "Namespace must not be null nor empty");
        if (attributes == null) {
            this.attributes = Collections.emptyMap();
        }
        else {
            this.attributes = attributes;
        }
        this.text = text;
        this.elements = elements;
    }

    @Override
    public String getElementName() {
        return name;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public String getAttributeValue(String attribute) {
        return attributes.get(attribute);
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public StandardExtensionElement getFirstElement(String element, String namespace) {
        if (elements == null) {
            return null;
        }
        QName key = new QName(namespace, element);
        return elements.getFirst(key);
    }

    public StandardExtensionElement getFirstElement(String element) {
        return getFirstElement(element, namespace);
    }

    public List<StandardExtensionElement> getElements(String element, String namespace) {
        if (elements == null) {
            return null;
        }
        QName key = new QName(namespace, element);
        return elements.getAll(key);
    }

    public List<StandardExtensionElement> getElements(String element) {
        return getElements(element, namespace);
    }

    public List<StandardExtensionElement> getElements() {
        if (elements == null) {
            return Collections.emptyList();
        }
        return elements.values();
    }

    public String getText() {
        return text;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        if (xmlCache != null) {
            return xmlCache;
        }
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            xml.attribute(entry.getKey(), entry.getValue());
        }
        xml.rightAngleBracket();

        if (text != null) {
            xml.text(text);
        }

        if (elements != null) {
            for (Map.Entry<QName, StandardExtensionElement> entry : elements.entrySet()) {
                xml.append(entry.getValue().toXML(getNamespace()));
            }
        }

        xml.closeElement(this);
        xmlCache = xml;
        return xml;
    }

    public static Builder builder(String name, String namespace) {
        return new Builder(name, namespace);
    }

    public static final class Builder {
        private final String name;
        private final String namespace;

        private Map<String, String> attributes;
        private String text;
        private MultiMap<QName, StandardExtensionElement> elements;

        private Builder(String name, String namespace) {
            this.name = name;
            this.namespace = namespace;
        }

        public Builder addAttribute(String name, String value) {
            StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
            Objects.requireNonNull(value, "Attribute value must be not null");
            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }
            attributes.put(name, value);
            return this;
        }

        public Builder addAttributes(Map<String, String> attributes) {
            if (this.attributes == null) {
                this.attributes = new LinkedHashMap<>(attributes.size());
            }
            this.attributes.putAll(attributes);
            return this;
        }

        public Builder setText(String text) {
            this.text = Objects.requireNonNull(text, "Text must be not null");
            return this;
        }

        public Builder addElement(StandardExtensionElement element) {
            Objects.requireNonNull(element, "Element must not be null");
            if (elements == null) {
                elements = new MultiMap<>();
            }

            QName key = element.getQName();
            elements.put(key, element);
            return this;
        }

        public Builder addElement(String name, String textValue) {
            StandardExtensionElement element = StandardExtensionElement.builder(name, this.namespace).setText(
                            textValue).build();
            return addElement(element);
        }

        public StandardExtensionElement build() {
            return new StandardExtensionElement(name, namespace, attributes, text, elements);
        }
    }
}
