/**
 *
 * Copyright 2016 Jules Tréhorel
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;

/**
 * An {@link ExtensionElement} storing the custom attributes listed in messages stanzas root <message>
 * element. As this use-case is a bad practice, this {@link ExtensionElement} allows XMPP clients to
 * retrieve those (wrongly set) values in a clean way.
 *
 * @author Jules Tréhorel
 */
public class CustomAttributesExtension implements ExtensionElement {
    public final static String NAME = "customAttributes";
    public final static String NAMESPACE = "http://smack.jivesoftware.com";

    private StandardExtensionElement extension;

	private CustomAttributesExtension(Map<String, String> properties) {
		if (properties == null || properties.isEmpty())
			return;

		StandardExtensionElement.Builder extensionBuilder = StandardExtensionElement.builder(NAME, NAMESPACE);
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			extensionBuilder.addElement(entry.getKey(), entry.getValue());
		}
        extension = extensionBuilder.build();
	}

    @Override
    public String getElementName() {
        return NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public boolean hasProperties() {
        return extension != null && extension.getElements().size() > 0;
    }

    public boolean hasProperty(String propertyKey) {
        return hasProperties() && extension.getElements(propertyKey) != null;
    }

    public String getPropertyValue(String propertyKey) {
        if (!hasProperty(propertyKey)) {
            return null;
        }
        return extension.getElements(propertyKey).get(0).getText(); // Only one element per key
    }

    @Override
    public XmlStringBuilder toXML() {
        return extension.toXML();
    }

    public static final class Builder {
        private final List<String> attributesWhiteList = Arrays.asList(new String[] {"xml:lang", "id", "to", "from", "type"});

        private Map<String, String> properties;

        public Builder() {
        }

        public Builder fromParser(XmlPullParser parser) {
            Objects.requireNonNull(parser, "Parser must be set");

            properties = new HashMap<String, String>();
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attributeName = parser.getAttributeName(i);
                boolean isLangAttribute = "lang".equals(attributeName) && "xml".equals(parser.getAttributePrefix(i));
                if (!attributesWhiteList.contains(attributeName) && !isLangAttribute) {
                    String attributeValue = parser.getAttributeValue(i);
                    properties.put(attributeName, attributeValue);
                }
            }
            return this;
        }

        public CustomAttributesExtension build() {
            return new CustomAttributesExtension(properties);
        }
    }
}
