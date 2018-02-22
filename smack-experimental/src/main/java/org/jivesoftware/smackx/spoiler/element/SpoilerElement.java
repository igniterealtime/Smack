/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.spoiler.element;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.spoiler.SpoilerManager;

public class SpoilerElement implements ExtensionElement {

    public static final String ELEMENT = "spoiler";
    public static final String NAMESPACE = SpoilerManager.NAMESPACE_0;

    public static final SpoilerElement EMPTY = new SpoilerElement(null, null);

    private final String hint;
    private final String language;

    /**
     * Create a new SpoilerElement with a hint about a content and a language attribute.
     *
     * @param language language of the hint.
     * @param hint hint about the content.
     */
    public SpoilerElement(String language, String hint) {
        if (StringUtils.isNotEmpty(language) && StringUtils.isNullOrEmpty(hint)) {
            throw new IllegalArgumentException("Hint cannot be null or empty if language is not empty.");
        }
        this.language = language;
        this.hint = hint;
    }

    /**
     * Return the hint text of the spoiler.
     * May be null.
     *
     * @return hint text
     */
    public String getHint() {
        return hint;
    }

    /**
     * Add a SpoilerElement to a message.
     *
     * @param message message to add the Spoiler to.
     */
    public static void addSpoiler(Message message) {
        message.addExtension(SpoilerElement.EMPTY);
    }

    /**
     * Add a SpoilerElement with a hint to a message.
     *
     * @param message Message to add the Spoiler to.
     * @param hint Hint about the Spoilers content.
     */
    public static void addSpoiler(Message message, String hint) {
        message.addExtension(new SpoilerElement(null, hint));
    }

    /**
     * Add a SpoilerElement with a hint in a certain language to a message.
     *
     * @param message Message to add the Spoiler to.
     * @param lang language of the Spoiler hint.
     * @param hint hint.
     */
    public static void addSpoiler(Message message, String lang, String hint) {
        message.addExtension(new SpoilerElement(lang, hint));
    }


    /**
     * Returns true, if the message has at least one spoiler element.
     *
     * @param message message
     * @return true if message has spoiler extension
     */
    public static boolean containsSpoiler(Message message) {
        return message.hasExtension(SpoilerElement.ELEMENT, NAMESPACE);
    }

    /**
     * Return a map of all spoilers contained in a message.
     * The map uses the language of a spoiler as key.
     * If a spoiler has no language attribute, its key will be an empty String.
     *
     * @param message message
     * @return map of spoilers
     */
    public static Map<String, String> getSpoilers(Message message) {
        if (!containsSpoiler(message)) {
            return Collections.emptyMap();
        }

        List<ExtensionElement> spoilers = message.getExtensions(SpoilerElement.ELEMENT, NAMESPACE);
        Map<String, String> map = new HashMap<>();

        for (ExtensionElement e : spoilers) {
            SpoilerElement s = (SpoilerElement) e;
            if (s.getLanguage() == null || s.getLanguage().equals("")) {
                map.put("", s.getHint());
            } else {
                map.put(s.getLanguage(), s.getHint());
            }
        }

        return map;
    }

    /**
     * Return the language of the hint.
     * May be null.
     *
     * @return language of hint text
     */
    public String getLanguage() {
        return language;
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
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optXmlLangAttribute(getLanguage());
        if (getHint() == null) {
            xml.closeEmptyElement();
        } else {
            xml.rightAngleBracket();
            xml.append(getHint());
            xml.closeElement(this);
        }
        return xml;
    }
}
