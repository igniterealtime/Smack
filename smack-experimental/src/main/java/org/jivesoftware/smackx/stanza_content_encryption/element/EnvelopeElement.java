/*
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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.address.packet.MultipleAddresses;
import org.jivesoftware.smackx.hints.element.MessageProcessingHint;
import org.jivesoftware.smackx.sid.element.StanzaIdElement;

import org.jxmpp.jid.Jid;

/**
 * Extension element that holds the content element, as well as a list of affix elements.
 * In SCE, the XML representation of this element is what will be encrypted using the encryption mechanism of choice.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class EnvelopeElement implements XmlElement {
    public static final String ELEMENT = "envelope";
    public static final String NAMESPACE = "urn:xmpp:sce:1";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final List<AffixElement> affixElements;
    private final ContentElement contentElement;

    EnvelopeElement(ContentElement content, List<AffixElement> affixElements) {
        this.contentElement = content;
        this.affixElements = Collections.unmodifiableList(affixElements);
    }

    /**
     * Return the {@link ContentElement} which holds the sensitive content extensions.
     *
     * @return content element
     */
    public ContentElement getContentElement() {
        return contentElement;
    }

    /**
     * Return a list of affix elements.
     * Those are elements that need to be verified upon reception by the encryption mechanisms implementation.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0420.html#affix_elements">
     *     XEP-0420: Stanza Content Encryption - §4. Affix Elements</a>
     *
     * @return list of affix elements
     */
    public List<AffixElement> getAffixElements() {
        return affixElements;
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
    public QName getQName() {
        return QNAME;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this).rightAngleBracket();
        xml.append(contentElement);
        xml.append(affixElements);
        return xml.closeElement(this);
    }

    /**
     * Return a {@link Builder} that can be used to build the {@link EnvelopeElement}.
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final Set<String> BLACKLISTED_NAMESPACES = Collections.singleton(MessageProcessingHint.NAMESPACE);
        private static final Set<QName> BLACKLISTED_QNAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                StanzaIdElement.QNAME,
                MultipleAddresses.QNAME
        )));

        private FromAffixElement from = null;
        private TimestampAffixElement timestamp = null;
        private RandomPaddingAffixElement rpad = null;

        private final List<AffixElement> otherAffixElements = new ArrayList<>();
        private final List<XmlElement> contentItems = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add an affix element of type 'to' which addresses one recipient.
         * The jid in the 'to' element SHOULD be a bare jid.
         *
         * @param jid jid
         * @return builder
         */
        public Builder addTo(Jid jid) {
            return addTo(new ToAffixElement(jid));
        }

        /**
         * Add an affix element of type 'to' which addresses one recipient.
         *
         * @param to affix element
         * @return builder
         */
        public Builder addTo(ToAffixElement to) {
            this.otherAffixElements.add(Objects.requireNonNull(to, "'to' affix element MUST NOT be null."));
            return this;
        }

        /**
         * Set the senders jid as a 'from' affix element.
         *
         * @param jid jid of the sender
         * @return builder
         */
        public Builder setFrom(Jid jid) {
            return setFrom(new FromAffixElement(jid));
        }

        /**
         * Set the senders jid as a 'from' affix element.
         *
         * @param from affix element
         * @return builder
         */
        public Builder setFrom(FromAffixElement from) {
            this.from = Objects.requireNonNull(from, "'form' affix element MUST NOT be null.");
            return this;
        }

        /**
         * Set the given date as a 'time' affix element.
         *
         * @param date timestamp as date
         * @return builder
         */
        public Builder setTimestamp(Date date) {
            return setTimestamp(new TimestampAffixElement(date));
        }

        /**
         * Set the timestamp of the message as a 'time' affix element.
         *
         * @param timestamp timestamp affix element
         * @return builder
         */
        public Builder setTimestamp(TimestampAffixElement timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "'time' affix element MUST NOT be null.");
            return this;
        }

        /**
         * Set some random length random content padding.
         *
         * @return builder
         */
        public Builder setRandomPadding() {
            this.rpad = new RandomPaddingAffixElement();
            return this;
        }

        /**
         * Set the given string as padding.
         * The padding should be of length between 1 and 200 characters.
         *
         * @param padding padding string
         * @return builder
         */
        public Builder setRandomPadding(String padding) {
            return setRandomPadding(new RandomPaddingAffixElement(padding));
        }

        /**
         * Set a padding affix element.
         *
         * @param padding affix element
         * @return builder
         */
        public Builder setRandomPadding(RandomPaddingAffixElement padding) {
            this.rpad = Objects.requireNonNull(padding, "'rpad' affix element MUST NOT be empty.");
            return this;
        }

        /**
         * Add an additional, SCE profile specific affix element.
         *
         * @param customAffixElement additional affix element
         * @return builder
         */
        public Builder addFurtherAffixElement(AffixElement customAffixElement) {
            this.otherAffixElements.add(Objects.requireNonNull(customAffixElement,
                    "Custom affix element MUST NOT be null."));
            return this;
        }

        /**
         * Add an item as child element of the content element.
         * There are some items that are not allowed as content.
         * Adding those will throw an exception.
         *
         * @see <a href="https://xmpp.org/extensions/xep-0420.html#server-processed">
         *     XEP-0420: Stanza Content Encryption - §9. Server-processed Elements</a>
         *
         * @param contentItem extension element
         * @return builder
         * @throws IllegalArgumentException in case an extension element from the blacklist is added.
         */
        public Builder addContentItem(XmlElement contentItem) {
            Objects.requireNonNull(contentItem, "Content item MUST NOT be null.");
            this.contentItems.add(checkForIllegalContentAndPossiblyThrow(contentItem));
            return this;
        }

        /**
         * Construct a content element from this builder.
         *
         * @return content element
         */
        public EnvelopeElement build() {
            List<AffixElement> allAffixElements = collectAffixElements();
            ContentElement contentElement = new ContentElement(contentItems);
            return new EnvelopeElement(contentElement, allAffixElements);
        }

        private static XmlElement checkForIllegalContentAndPossiblyThrow(XmlElement contentItem) {
            QName qName = contentItem.getQName();
            if (BLACKLISTED_QNAMES.contains(qName)) {
                throw new IllegalArgumentException("Element identified by " + qName +
                        " is not allowed as content item. See https://xmpp.org/extensions/xep-0420.html#server-processed");
            }

            String namespace = contentItem.getNamespace();
            if (BLACKLISTED_NAMESPACES.contains(namespace)) {
                throw new IllegalArgumentException("Elements of namespace '" + namespace +
                        "' are not allowed as content items. See https://xmpp.org/extensions/xep-0420.html#server-processed");
            }
            return contentItem;
        }

        private List<AffixElement> collectAffixElements() {
            List<AffixElement> allAffixElements = new ArrayList<>(Arrays.asList(rpad, from, timestamp));
            allAffixElements.addAll(otherAffixElements);
            return allAffixElements;
        }
    }
}
