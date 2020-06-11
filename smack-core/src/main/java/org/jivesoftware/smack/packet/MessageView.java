/**
 *
 * Copyright 2003-2007 Jive Software, 2019-2020 Florian Schmaus
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.Message.Subject;
import org.jivesoftware.smack.util.Objects;

public interface MessageView extends StanzaView {

    /**
     * Returns the type of the message. If no type has been set this method will return {@link
     * org.jivesoftware.smack.packet.Message.Type#normal}.
     *
     * @return the type of the message.
     */
    Message.Type getType();

    /**
     * Returns the default subject of the message, or null if the subject has not been set.
     * The subject is a short description of message contents.
     * <p>
     * The default subject of a message is the subject that corresponds to the message's language.
     * (see {@link #getLanguage()}) or if no language is set to the applications default
     * language (see {@link Stanza#getDefaultLanguage()}).
     *
     * @return the subject of the message.
     */
    default String getSubject() {
        return getSubject(null);
    }

    /**
     * Returns the subject corresponding to the language. If the language is null, the method result
     * will be the same as {@link #getSubject()}. Null will be returned if the language does not have
     * a corresponding subject.
     *
     * @param language the language of the subject to return.
     * @return the subject related to the passed in language.
     */
    default String getSubject(String language) {
        Subject subject = getMessageSubject(language);
        return subject == null ? null : subject.getSubject();
    }

    default Message.Subject getMessageSubject(String language) {
        language = Stanza.determineLanguage(this, language);
        for (Message.Subject subject : getSubjects()) {
            if (Objects.equals(language, subject.getLanguage())
                            || (subject.getLanguage() == null && Objects.equals(getLanguage(), language))) {
                return subject;
            }
        }
        return null;
    }

    /**
     * Returns a set of all subjects in this Message, including the default message subject accessible
     * from {@link #getSubject()}.
     *
     * @return a collection of all subjects in this message.
     */
    default Set<Message.Subject> getSubjects() {
        List<Message.Subject> subjectList = getExtensions(Subject.class);

        Set<Message.Subject> subjects = new HashSet<>(subjectList.size());
        subjects.addAll(subjectList);

        return subjects;
    }

    /**
     * Returns all the languages being used for the subjects, not including the default subject.
     *
     * @return the languages being used for the subjects.
     */
    default List<String> getSubjectLanguages() {
        Message.Subject defaultSubject = getMessageSubject(null);
        List<String> languages = new ArrayList<String>();
        for (Message.Subject subject : getExtensions(Message.Subject.class)) {
            if (!subject.equals(defaultSubject)) {
                languages.add(subject.getLanguage());
            }
        }
        return Collections.unmodifiableList(languages);
    }

    /**
     * Returns the default body of the message, or null if the body has not been set. The body
     * is the main message contents.
     * <p>
     * The default body of a message is the body that corresponds to the message's language.
     * (see {@link #getLanguage()}) or if no language is set to the applications default
     * language (see {@link Stanza#getDefaultLanguage()}).
     *
     * @return the body of the message.
     */
    default String getBody() {
        return getBody(getLanguage());
    }

    /**
     * Returns the body corresponding to the language. If the language is null, the method result
     * will be the same as {@link #getBody()}. Null will be returned if the language does not have
     * a corresponding body.
     *
     * @param language the language of the body to return.
     * @return the body related to the passed in language.
     * @since 3.0.2
     */
    default String getBody(String language) {
        Message.Body body = getMessageBody(language);
        return body == null ? null : body.getMessage();
    }

    default Message.Body getMessageBody(String language) {
        language = Stanza.determineLanguage(this, language);
        for (Message.Body body : getBodies()) {
            if (Objects.equals(language, body.getLanguage()) || (language != null && language.equals(getLanguage()) && body.getLanguage() == null)) {
                return body;
            }
        }
        return null;
    }

    /**
     * Returns a set of all bodies in this Message, including the default message body accessible
     * from {@link #getBody()}.
     *
     * @return a collection of all bodies in this Message.
     * @since 3.0.2
     */
    default Set<Message.Body> getBodies() {
        List<ExtensionElement> bodiesList = getExtensions(Message.Body.QNAME);
        Set<Message.Body> resultSet = new HashSet<>(bodiesList.size());
        for (ExtensionElement extensionElement : bodiesList) {
            Message.Body body = (Message.Body) extensionElement;
            resultSet.add(body);
        }
        return resultSet;
    }

    /**
     * Returns all the languages being used for the bodies, not including the default body.
     *
     * @return the languages being used for the bodies.
     * @since 3.0.2
     */
    default List<String> getBodyLanguages() {
        Message.Body defaultBody = getMessageBody(null);
        List<String> languages = new ArrayList<String>();
        for (Message.Body body : getBodies()) {
            if (!body.equals(defaultBody)) {
                languages.add(body.getLanguage());
            }
        }
        return Collections.unmodifiableList(languages);
    }

    /**
     * Returns the thread id of the message, which is a unique identifier for a sequence
     * of "chat" messages. If no thread id is set, <code>null</code> will be returned.
     *
     * @return the thread id of the message, or <code>null</code> if it doesn't exist.
     */
    default String getThread() {
        Message.Thread thread = getExtension(Message.Thread.class);
        if (thread == null) {
            return null;
        }
        return thread.getThread();
    }
}
