/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.mood.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mood.Mood;

/**
 * {@link ExtensionElement} that contains the users mood.
 *
 * Optionally this element also contains a text node, which contains a natural language description or
 * reason for the mood.
 */
public class MoodElement implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/mood";
    public static final String ELEMENT = "mood";
    public static final String ELEM_TEXT = "text";

    private final MoodSubjectElement mood;
    private final String text;

    public MoodElement(MoodSubjectElement mood, String text) {
        if (mood == null && text != null) {
            throw new IllegalArgumentException("If <mood/> is null, text MUST be null too.");
        }

        this.mood = mood;
        this.text = text;
    }

    /**
     * Return the senders mood.
     * This method returns null in case the sender wants to stop sending their mood.
     *
     * @return mood or null
     */
    public Mood getMood() {
        return mood != null ? mood.getMood() : null;
    }

    /**
     * The user might set a reason or description for/of their mood.
     * This method returns a natural language reason for the mood.
     *
     * @return text or null.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns true, if the user gives a reason for their mood.
     *
     * @return true or false
     */
    public boolean hasText() {
        return getText() != null;
    }

    /**
     * Implementors might implement custom concretisations of mood.
     * This method returns any custom concretisation of the mood the user might have set.
     *
     * @return concretisation or null.
     */
    public MoodConcretisation getMoodConcretisation() {
        return mood != null ? mood.getConcretisation() : null;
    }

    /**
     * Return true, if this mood has a concretisation.
     *
     * @return true or false
     */
    public boolean hasConcretisation() {
        return getMoodConcretisation() != null;
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
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);

        if (mood == null && text == null) {
            // Empty mood element used as STOP signal
            return xml.closeEmptyElement();
        }
        xml.rightAngleBracket();

        xml.optAppend(mood);

        if (text != null) {
            xml.openElement(ELEM_TEXT)
                    .append(text)
                    .closeElement(ELEM_TEXT);
        }
        return xml.closeElement(getElementName());
    }

    /**
     * Extract a {@link MoodElement} from a message.
     *
     * @param message message
     *
     * @return {@link MoodElement} or null.
     */
    public static MoodElement fromMessage(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * Return true, if the {@code message} has a {@link MoodElement}, otherwise false.
     *
     * @param message message
     *
     * @return true of false
     */
    public static boolean hasMoodElement(Message message) {
        return message.hasExtension(ELEMENT, NAMESPACE);
    }

    /**
     * {@link NamedElement} which represents the mood.
     * This element has the element name of the mood selected from {@link Mood}.
     */
    public static class MoodSubjectElement implements NamedElement {

        private final Mood mood;
        private final MoodConcretisation concretisation;

        public MoodSubjectElement(Mood mood, MoodConcretisation concretisation) {
            this.mood = Objects.requireNonNull(mood);
            this.concretisation = concretisation;
        }

        @Override
        public String getElementName() {
            return mood.toString();
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();

            if (concretisation == null) {
                return xml.emptyElement(getElementName());
            }

            return xml.openElement(getElementName())
                    .append(concretisation.toXML())
                    .closeElement(getElementName());
        }

        /**
         * Return the mood of the user.
         *
         * @return mood or null
         */
        public Mood getMood() {
            return mood;
        }

        /**
         * Return the concretisation of the mood.
         *
         * @return concretisation or null
         */
        public MoodConcretisation getConcretisation() {
            return concretisation;
        }
    }
}
