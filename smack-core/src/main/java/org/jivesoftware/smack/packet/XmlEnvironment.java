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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

public class XmlEnvironment {

    public static final XmlEnvironment EMPTY = new XmlEnvironment((String) null);

    private final String namespace;
    private final String language;
    private final XmlEnvironment next;

    private transient boolean effectiveNamespaceDetermined;
    private transient boolean effectiveLanguageDetermined;
    private transient String effectiveNamespace;
    private transient String effectiveLanguage;

    public XmlEnvironment(String namespace) {
        this(namespace, null);
    }

    public XmlEnvironment(String namespace, String language) {
        this(namespace, language, null);
    }

    private XmlEnvironment(Builder builder) {
        this(builder.namespace, builder.language, builder.next);
    }

    public XmlEnvironment(String namespace, String language, XmlEnvironment next) {
        this.namespace = namespace;
        this.language = language;
        this.next = next;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getEffectiveNamespace() {
        if (effectiveNamespaceDetermined) {
            return effectiveNamespace;
        }

        if (StringUtils.isNotEmpty(namespace)) {
            effectiveNamespace = namespace;
        } else if (next != null) {
            effectiveNamespace = next.getEffectiveNamespace();
        }

        effectiveNamespaceDetermined = true;
        return effectiveNamespace;
    }

    public String getEffectiveNamespaceOrUse(String namespace) {
        String effectiveNamespace = getEffectiveNamespace();
        if (StringUtils.isNullOrEmpty(effectiveNamespace)) {
            return namespace;
        }
        return effectiveNamespace;
    }

    public boolean effectiveNamespaceEquals(String namespace) {
        String effectiveNamespace = getEffectiveNamespace();
        if (effectiveNamespace == null) {
            return false;
        }
        return effectiveNamespace.equals(namespace);
    }

    public String getLanguage() {
        return language;
    }

    public String getEffectiveLanguage() {
        if (effectiveLanguageDetermined) {
            return effectiveLanguage;
        }

        if (StringUtils.isNotEmpty(language)) {
            effectiveLanguage = language;
        } else if (next != null) {
            effectiveLanguage = next.getEffectiveLanguage();
        }

        effectiveLanguageDetermined = true;
        return effectiveLanguage;
    }

    public boolean effectiveLanguageEquals(String language) {
        String effectiveLanguage = getEffectiveLanguage();
        if (effectiveLanguage == null) {
            return false;
        }
        return effectiveLanguage.equals(language);
    }

    private transient String toStringCache;

    @Override
    public String toString() {
        if (toStringCache == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(XmlEnvironment.class.getSimpleName()).append(' ');
            sb.append("xmlns=").append(getEffectiveNamespace()).append(' ');
            sb.append("xmllang=").append(getEffectiveLanguage()).append(' ');

            toStringCache = sb.toString();
        }
        return toStringCache;
    }

    public static XmlEnvironment from(XmlPullParser parser) {
        return from(parser, null);
    }

    public static XmlEnvironment from(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) {
        String namespace = parser.getNamespace();
        String xmlLang = ParserUtils.getXmlLang(parser);
        return new XmlEnvironment(namespace, xmlLang, outerXmlEnvironment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String namespace;
        private String language;
        private XmlEnvironment next;

        public Builder withNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withLanguage(String language) {
            this.language = language;
            return this;
        }

        public Builder withNext(XmlEnvironment next) {
            this.next = next;
            return this;
        }

        public Builder with(StreamOpen streamOpen) {
            withNamespace(streamOpen.getNamespace());
            withLanguage(streamOpen.getLanguage());
            return this;
        }

        public XmlEnvironment build() {
            return new XmlEnvironment(this);
        }
    }
}
