/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.message_fastening.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.message_fastening.MessageFasteningManager;
import org.jivesoftware.smackx.sid.element.OriginIdElement;

/**
 * Message Fastening container element.
 */
public final class FasteningElement implements ExtensionElement {

    public static final String ELEMENT = "apply-to";
    public static final String NAMESPACE = MessageFasteningManager.NAMESPACE;
    public static final String ATTR_ID = "id";
    public static final String ATTR_CLEAR = "clear";
    public static final String ATTR_SHELL = "shell";

    private final OriginIdElement referencedStanzasOriginId;
    private final List<ExternalElement> externalPayloads = new ArrayList<>();
    private final List<ExtensionElement> wrappedPayloads = new ArrayList<>();
    private final boolean clear;
    private final boolean shell;

    private FasteningElement(OriginIdElement originId,
                             List<ExtensionElement> wrappedPayloads,
                             List<ExternalElement> externalPayloads,
                             boolean clear,
                             boolean shell) {
        this.referencedStanzasOriginId = Objects.requireNonNull(originId, "Fastening element MUST contain an origin-id.");
        this.wrappedPayloads.addAll(wrappedPayloads);
        this.externalPayloads.addAll(externalPayloads);
        this.clear = clear;
        this.shell = shell;
    }

    /**
     * Return the {@link OriginIdElement origin-id} of the {@link Stanza} that the message fastenings are to be
     * applied to.
     *
     * @return origin id of the referenced stanza
     */
    public OriginIdElement getReferencedStanzasOriginId() {
        return referencedStanzasOriginId;
    }

    /**
     * Return all wrapped payloads of this element.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#wrapped-payloads">XEP-0422: §3.1. Wrapped Payloads</a>
     *
     * @return wrapped payloads.
     */
    public List<ExtensionElement> getWrappedPayloads() {
        return Collections.unmodifiableList(wrappedPayloads);
    }

    /**
     * Return all external payloads of this element.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#external-payloads">XEP-0422: §3.2. External Payloads</a>
     *
     * @return external payloads.
     */
    public List<ExternalElement> getExternalPayloads() {
        return Collections.unmodifiableList(externalPayloads);
    }

    /**
     * Does this element remove a previously sent {@link FasteningElement}?
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#remove">
     *     XEP-0422: Message Fastening §3.4 Removing fastenings</a>
     *
     * @return true if the clear attribute is set.
     */
    public boolean isRemovingElement() {
        return clear;
    }

    /**
     * Is this a shell element?
     * Shell elements are otherwise empty elements that indicate that an encrypted payload of a message
     * encrypted using XEP-420: Stanza Content Encryption contains a sensitive {@link FasteningElement}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#encryption">
     *     XEP-0422: Message Fastening §3.5 Interaction with stanza encryption</a>
     *
     * @return true if this is a shell element.
     */
    public boolean isShellElement() {
        return shell;
    }

    /**
     * Return true if the provided {@link Message} contains a {@link FasteningElement}.
     *
     * @param message message
     * @return true if the stanza has an {@link FasteningElement}.
     */
    public static boolean hasFasteningElement(Message message) {
        return message.hasExtension(ELEMENT, MessageFasteningManager.NAMESPACE);
    }

    /**
     * Return true if the provided {@link MessageBuilder} contains a {@link FasteningElement}.
     *
     * @param builder message builder
     * @return true if the stanza has an {@link FasteningElement}.
     */
    public static boolean hasFasteningElement(MessageBuilder builder) {
        return builder.hasExtension(FasteningElement.class);
    }

    @Override
    public String getNamespace() {
        return MessageFasteningManager.NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this)
                .attribute(ATTR_ID, referencedStanzasOriginId.getId())
                .optBooleanAttribute(ATTR_CLEAR, isRemovingElement())
                .optBooleanAttribute(ATTR_SHELL, isShellElement())
                .rightAngleBracket();
        addPayloads(xml);
        return xml.closeElement(this);
    }

    private void addPayloads(XmlStringBuilder xml) {
        for (ExternalElement external : externalPayloads) {
            xml.append(external);
        }
        for (ExtensionElement wrapped : wrappedPayloads) {
            xml.append(wrapped);
        }
    }

    public static FasteningElement createShellElementForSensitiveElement(FasteningElement sensitiveElement) {
        return createShellElementForSensitiveElement(sensitiveElement.getReferencedStanzasOriginId());
    }

    public static FasteningElement createShellElementForSensitiveElement(String originIdOfSensitiveElement) {
        return createShellElementForSensitiveElement(new OriginIdElement(originIdOfSensitiveElement));
    }

    public static FasteningElement createShellElementForSensitiveElement(OriginIdElement originIdOfSensitiveElement) {
        return FasteningElement.builder()
                .setOriginId(originIdOfSensitiveElement)
                .setShell()
                .build();
    }

    /**
     * Add this element to the provided message builder.
     * Note: The stanza MUST NOT contain more than one apply-to elements at the same time.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0422.html#rules">XEP-0422 §4: Business Rules</a>
     *
     * @param messageBuilder message builder
     */
    public void applyTo(MessageBuilder messageBuilder) {
        if (FasteningElement.hasFasteningElement(messageBuilder)) {
            throw new IllegalArgumentException("Stanza cannot contain more than one apply-to elements.");
        } else {
            messageBuilder.addExtension(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OriginIdElement originId;
        private final List<ExtensionElement> wrappedPayloads = new ArrayList<>();
        private final List<ExternalElement> externalPayloads = new ArrayList<>();
        private boolean isClear = false;
        private boolean isShell = false;

        /**
         * Set the origin-id of the referenced message.
         *
         * @param originIdString origin id as String
         * @return builder instance
         */
        public Builder setOriginId(String originIdString) {
            return setOriginId(new OriginIdElement(originIdString));
        }

        /**
         * Set the {@link OriginIdElement} of the referenced message.
         *
         * @param originId origin-id as element
         * @return builder instance
         */
        public Builder setOriginId(OriginIdElement originId) {
            this.originId = originId;
            return this;
        }

        /**
         * Add a wrapped payload.
         *
         * @param wrappedPayload wrapped payload
         * @return builder instance
         */
        public Builder addWrappedPayload(ExtensionElement wrappedPayload) {
            return addWrappedPayloads(Collections.singletonList(wrappedPayload));
        }

        /**
         * Add multiple wrapped payloads at once.
         *
         * @param wrappedPayloads list of wrapped payloads
         * @return builder instance
         */
        public Builder addWrappedPayloads(List<ExtensionElement> wrappedPayloads) {
            this.wrappedPayloads.addAll(wrappedPayloads);
            return this;
        }

        /**
         * Add an external payload.
         *
         * @param externalPayload external payload
         * @return builder instance
         */
        public Builder addExternalPayload(ExternalElement externalPayload) {
            return addExternalPayloads(Collections.singletonList(externalPayload));
        }

        /**
         * Add multiple external payloads at once.
         *
         * @param externalPayloads external payloads
         * @return builder instance
         */
        public Builder addExternalPayloads(List<ExternalElement> externalPayloads) {
            this.externalPayloads.addAll(externalPayloads);
            return this;
        }

        /**
         * Declare this {@link FasteningElement} to remove previous fastenings.
         * Semantically the wrapped payloads of this element declares all wrapped payloads from the referenced
         * fastening element that share qualified names as removed.
         *
         * @see <a href="https://xmpp.org/extensions/xep-0422.html#remove">
         *     XEP-0422: Message Fastening §3.4 Removing fastenings</a>
         *
         * @return builder instance
         */
        public Builder setClear() {
            isClear = true;
            return this;
        }

        /**
         * Declare this {@link FasteningElement} to be a shell element.
         * Shell elements are used as hints that a Stanza Content Encryption payload contains another sensitive
         * {@link FasteningElement}. The outer "shell" {@link FasteningElement} is used to do fastening collation.
         *
         * @see <a href="https://xmpp.org/extensions/xep-0422.html#encryption">XEP-0422: Message Fastening §3.5 Interaction with stanza encryption</a>
         * @see <a href="https://xmpp.org/extensions/xep-0420.html">XEP-0420: Stanza Content Encryption</a>
         *
         * @return builder instance
         */
        public Builder setShell() {
            isShell = true;
            return this;
        }

        /**
         * Build the element.
         * @return built element.
         */
        public FasteningElement build() {
            validateThatIfIsShellThenOtherwiseEmpty();
            return new FasteningElement(originId, wrappedPayloads, externalPayloads, isClear, isShell);
        }

        private void validateThatIfIsShellThenOtherwiseEmpty() {
            if (!isShell) {
                return;
            }

            if (isClear || !wrappedPayloads.isEmpty() || !externalPayloads.isEmpty()) {
                throw new IllegalArgumentException("A fastening that is a shell element must be otherwise empty " +
                        "and cannot have a 'clear' attribute.");
            }
        }
    }
}
