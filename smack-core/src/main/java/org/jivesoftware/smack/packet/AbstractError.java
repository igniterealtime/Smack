/**
 *
 * Copyright 2014 Florian Schmaus
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.util.PacketUtil;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class AbstractError {

    private final String textNamespace;
    protected final Map<String, String> descriptiveTexts;
    private final List<ExtensionElement> extensions;


    protected AbstractError(Map<String, String> descriptiveTexts) {
        this(descriptiveTexts, null);
    }

    protected AbstractError(Map<String, String> descriptiveTexts, List<ExtensionElement> extensions) {
        this(descriptiveTexts, null, extensions);
    }

    protected AbstractError(Map<String, String> descriptiveTexts, String textNamespace, List<ExtensionElement> extensions) {
        if (descriptiveTexts != null) {
            this.descriptiveTexts = descriptiveTexts;
        } else {
            this.descriptiveTexts = Collections.emptyMap();
        }
        this.textNamespace = textNamespace;
        if (extensions != null) {
            this.extensions = extensions;
        } else {
            this.extensions = Collections.emptyList();
        }
    }

    /**
     * Get the descriptive text of this SASLFailure.
     * <p>
     * Returns the descriptive text of this SASLFailure in the system default language if possible. May return null.
     * </p>
     * 
     * @return the descriptive text or null.
     */
    public String getDescriptiveText() {
        String defaultLocale = Locale.getDefault().getLanguage();
        String descriptiveText = getDescriptiveText(defaultLocale);
        if (descriptiveText == null) {
            descriptiveText = getDescriptiveText("");
        }
        return descriptiveText;
    }

    /**
     * Get the descriptive test of this SASLFailure.
     * <p>
     * Returns the descriptive text of this SASLFailure in the given language. May return null if not available.
     * </p>
     * 
     * @param xmllang the language.
     * @return the descriptive text or null.
     */
    public String getDescriptiveText(String xmllang) {
        return descriptiveTexts.get(xmllang);
    }

    /**
     * Returns the first stanza(/packet) extension that matches the specified element name and
     * namespace, or <tt>null</tt> if it doesn't exist. 
     *
     * @param elementName the XML element name of the stanza(/packet) extension.
     * @param namespace the XML element namespace of the stanza(/packet) extension.
     * @return the extension, or <tt>null</tt> if it doesn't exist.
     */
    public <PE extends ExtensionElement> PE getExtension(String elementName, String namespace) {
        return PacketUtil.extensionElementFrom(extensions, elementName, namespace);
    }

    protected void addDescriptiveTextsAndExtensions(XmlStringBuilder xml) {
        for (Map.Entry<String, String> entry : descriptiveTexts.entrySet()) {
            String xmllang = entry.getKey();
            String text = entry.getValue();
            xml.halfOpenElement("text").xmlnsAttribute(textNamespace)
                    .xmllangAttribute(xmllang).rightAngleBracket();
            xml.escape(text);
            xml.closeElement("text");
        }
        for (ExtensionElement packetExtension : extensions) {
            xml.append(packetExtension.toXML());
        }
    }
}
