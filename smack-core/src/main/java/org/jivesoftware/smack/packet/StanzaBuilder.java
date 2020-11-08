/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.Function;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.ToStringUtil;
import org.jivesoftware.smack.util.XmppElementUtil;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public abstract class StanzaBuilder<B extends StanzaBuilder<B>> implements StanzaView {

    final StanzaIdSource stanzaIdSource;
    final String stanzaId;

    Jid to;
    Jid from;

    StanzaError stanzaError;

    String language;

    MultiMap<QName, ExtensionElement> extensionElements = new MultiMap<>();

    protected StanzaBuilder(StanzaBuilder<?> other) {
        stanzaIdSource = other.stanzaIdSource;
        stanzaId = other.stanzaId;

        to = other.to;
        from = other.from;
        stanzaError = other.stanzaError;
        language = other.language;
        extensionElements = other.extensionElements.clone();
    }

    protected StanzaBuilder(StanzaIdSource stanzaIdSource) {
        this.stanzaIdSource = stanzaIdSource;
        this.stanzaId = null;
    }

    protected StanzaBuilder(String stanzaId) {
        this.stanzaIdSource = null;
        this.stanzaId = StringUtils.requireNullOrNotEmpty(stanzaId, "Stanza ID must not be the empty String");
    }

    protected StanzaBuilder(Stanza message, String stanzaId) {
        this(stanzaId);
        copyFromStanza(message);
    }

    protected StanzaBuilder(Stanza message, StanzaIdSource stanzaIdSource) {
        this(stanzaIdSource);
        copyFromStanza(message);
    }

    private void copyFromStanza(Stanza stanza) {
        to = stanza.getTo();
        from = stanza.getFrom();
        stanzaError = stanza.getError();
        language = stanza.getLanguage();

        extensionElements = stanza.cloneExtensionsMap();
    }

    /**
     * Set the recipent address of the stanza.
     *
     * @param to whoe the stanza is being sent to.
     * @return a reference to this builder.
     * @throws XmppStringprepException if the provided character sequence is not a valid XMPP address.
     * @see #to(Jid)
     */
    public final B to(CharSequence to) throws XmppStringprepException {
        return to(JidCreate.from(to));
    }

    /**
     * Sets who the stanza is being sent "to". The XMPP protocol often makes the "to" attribute optional, so it does not
     * always need to be set.
     *
     * @param to who the stanza is being sent to.
     * @return a reference to this builder.
     */
    public final B to(Jid to) {
        this.to = to;
        return getThis();
    }

    /**
     * Sets who the the stanza is being sent "from".
     *
     * @param from who the stanza is being sent from.
     * @return a reference to this builder.
     * @throws XmppStringprepException if the provided character sequence is not a valid XMPP address.
     * @see #from(Jid)
     */
    public final B from(CharSequence from) throws XmppStringprepException {
        return from(JidCreate.from(from));
    }

    /**
     * Sets who the stanza is being sent "from". The XMPP protocol often makes the "from" attribute optional, so it does
     * not always need to be set.
     *
     * @param from who the stanza is being sent from.
     * @return a reference to this builder.
     */
    public final B from(Jid from) {
        this.from = from;
        return getThis();
    }

    /**
     * Sets the error for this stanza.
     *
     * @param stanzaError the error to associate with this stanza.
     * @return a reference to this builder.
     */
    public final B setError(StanzaError stanzaError) {
        this.stanzaError = stanzaError;
        return getThis();
    }

    /**
     * Sets the xml:lang for this stanza.
     *
     * @param language the xml:lang of this stanza.
     * @return a reference to this builder.
     */
    public final B setLanguage(String language) {
        this.language = language;
        return getThis();
    }

    public final B addExtension(ExtensionElement extensionElement) {
        QName key = extensionElement.getQName();
        extensionElements.put(key, extensionElement);
        return getThis();
    }

    public final B addOptExtensions(Collection<? extends ExtensionElement> extensionElements) {
        if (extensionElements == null) {
            return getThis();
        }

        return addExtensions(extensionElements);
    }

    public final B addExtensions(Collection<? extends ExtensionElement> extensionElements) {
        for (ExtensionElement extensionElement : extensionElements) {
            addExtension(extensionElement);
        }
        return getThis();
    }

    public final B overrideExtension(ExtensionElement extensionElement) {
        QName key = extensionElement.getQName();
        extensionElements.remove(key);
        extensionElements.put(key, extensionElement);
        return getThis();
    }

    public abstract Stanza build();

    public abstract B getThis();

    @Override
    public final String getStanzaId() {
        return stanzaId;
    }

    @Override
    public final Jid getTo() {
        return to;
    }

    @Override
    public final Jid getFrom() {
        return from;
    }

    @Override
    public final String getLanguage() {
        return language;
    }

    @Override
    public final StanzaError getError() {
        return stanzaError;
    }

    @Override
    public final ExtensionElement getExtension(QName qname) {
        return extensionElements.getFirst(qname);
    }

    @Override
    public final List<ExtensionElement> getExtensions() {
        return extensionElements.values();
    }

    @Override
    public final List<ExtensionElement> getExtensions(QName qname) {
        return extensionElements.getAll(qname);
    }

    @Override
    public final <E extends ExtensionElement> List<E> getExtensions(Class<E> extensionElementClass) {
        return XmppElementUtil.getElementsFrom(extensionElements, extensionElementClass);
    }

    public final boolean willBuildStanzaWithId() {
        return stanzaIdSource != null || StringUtils.isNotEmpty(stanzaId);
    }

    public final void throwIfNoStanzaId() {
        if (willBuildStanzaWithId()) {
            return;
        }
        throw new IllegalArgumentException(
                        "The builder will not build a stanza with an ID set, although it is required");
    }

    protected abstract void addStanzaSpecificAttributes(ToStringUtil.Builder builder);

    @Override
    public final String toString() {
        ToStringUtil.Builder builder = ToStringUtil.builderFor(getClass())
            .addValue("id", stanzaId)
            .addValue("from", from)
            .addValue("to", to)
            .addValue("language", language)
            .addValue("error", stanzaError)
            ;

        addStanzaSpecificAttributes(builder);

        builder.add("Extension Elements", extensionElements.values(), e -> {
            return e.getQName();
        });

        return builder.build();
    }

    public static MessageBuilder buildMessage() {
        return buildMessage(null);
    }

    public static MessageBuilder buildMessage(String stanzaId) {
        return new MessageBuilder(stanzaId);
    }

    public static MessageBuilder buildMessageFrom(Message message, String stanzaId) {
        return new MessageBuilder(message, stanzaId);
    }

    public static MessageBuilder buildMessageFrom(Message message, StanzaIdSource stanzaIdSource) {
        return new MessageBuilder(message, stanzaIdSource);
    }

    public static PresenceBuilder buildPresence() {
        return buildPresence(null);
    }

    public static PresenceBuilder buildPresence(String stanzaId) {
        return new PresenceBuilder(stanzaId);
    }

    public static PresenceBuilder buildPresenceFrom(Presence presence, String stanzaId) {
        return new PresenceBuilder(presence, stanzaId);
    }

    public static PresenceBuilder buildPresenceFrom(Presence presence, StanzaIdSource stanzaIdSource) {
        return new PresenceBuilder(presence, stanzaIdSource);
    }

    public static IqData buildIqData(String stanzaId) {
        return new IqData(stanzaId);
    }

    public static <SB extends StanzaBuilder<?>> SB buildResponse(StanzaView request, Function<SB, String> builderFromStanzaId) {
        SB responseBuilder = builderFromStanzaId.apply(request.getStanzaId());

        responseBuilder.to(request.getFrom())
            .from(request.getTo())
            ;

        return responseBuilder;
    }

}
