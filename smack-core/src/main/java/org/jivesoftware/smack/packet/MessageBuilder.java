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

import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smack.packet.Message.Subject;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.ToStringUtil;

public final class MessageBuilder extends MessageOrPresenceBuilder<Message, MessageBuilder> implements MessageView {
    static final MessageBuilder EMPTY = new MessageBuilder(() -> {
        return null;
    });

    Message.Type type;

    String thread;

    MessageBuilder(Message message, String stanzaId) {
        super(message, stanzaId);
        copyFromMessage(message);
    }

    MessageBuilder(Message message, StanzaIdSource stanzaIdSource) {
        super(message, stanzaIdSource);
        copyFromMessage(message);
    }

    MessageBuilder(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    MessageBuilder(String stanzaId) {
        super(stanzaId);
    }

    private void copyFromMessage(Message message) {
        type = message.getType();
        thread = message.getThread();
    }

    @Override
    protected void addStanzaSpecificAttributes(ToStringUtil.Builder builder) {
        builder.addValue("type", type)
               .addValue("thread", thread)
               ;
    }

    public MessageBuilder ofType(Message.Type type) {
        this.type = type;
        return getThis();
    }

    public MessageBuilder setThread(String thread) {
        this.thread = thread;
        return getThis();
    }

    /**
     * Sets the subject of the message. The subject is a short description of
     * message contents.
     *
     * @param subject the subject of the message.
     * @return a reference to this builder.
     */
    public MessageBuilder setSubject(String subject) {
        return addSubject(null, subject);
    }

    /**
     * Adds a subject with a corresponding language.
     *
     * @param language the language of the subject being added.
     * @param subject the subject being added to the message.
     * @return a reference to this builder.
     * @throws NullPointerException if the subject is null.
     */
    public MessageBuilder addSubject(String language, String subject) {
        language = StringUtils.requireNullOrNotEmpty(language, "language must be null or not empty");

        for (Subject currentSubject : getExtensions(Subject.class)) {
            if (StringUtils.nullSafeCharSequenceEquals(language, currentSubject.getLanguage())) {
                throw new IllegalArgumentException("Subject with the language " + language + " already exists");
            }
        }

        Subject messageSubject = new Subject(language, subject);
        addExtension(messageSubject);

        return this;
    }

    /**
     * Sets the body of the message.
     *
     * @param body the body of the message.
     * @return a reference to this builder.
     * @see #setBody(String)
     */
    public MessageBuilder setBody(CharSequence body) {
        return setBody(body.toString());
    }

    /**
     * Sets the body of the message. The body is the main message contents.
     *
     * @param body the body of the message.
     * @return a reference to this builder.
     */
    public MessageBuilder setBody(String body) {
        return addBody(null, body);
    }

    /**
     * Adds a body with a corresponding language.
     *
     * @param language the language of the body being added.
     * @param body the body being added to the message.
     * @return a reference to this builder.
     */
    public MessageBuilder addBody(String language, String body) {
        language = StringUtils.requireNullOrNotEmpty(language, "language must be null or not empty");

        for (Body currentBody : getExtensions(Body.class)) {
            if (StringUtils.nullSafeCharSequenceEquals(language, currentBody.getLanguage())) {
                throw new IllegalArgumentException("Bodyt with the language " + language + " already exists");
            }
        }

        Body messageBody = new Body(language, body);
        addExtension(messageBody);

        return this;
    }

    @Override
    public MessageBuilder getThis() {
        return this;
    }

    @Override
    public Message build() {
        return new Message(this);
    }

    @Override
    public Message.Type getType() {
        return type;
    }
}
