/**
 *
 * Copyright 2015-2016 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import java.util.Date;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * The configuration used to enter a MUC room. This configuration is usually used when joining an
 * existing room. When creating a new room, only the Nickname setting is relevant.
 * <p>
 * A builder for this can be obtained by calling {@link MultiUserChat#getEnterConfigurationBuilder(Resourcepart)}.
 * </p>
 * 
 * @author Florian Schmaus
 * @since 4.2
 */
public final class MucEnterConfiguration {

    private final Resourcepart nickname;
    private final String password;
    private final int maxChars;
    private final int maxStanzas;
    private final int seconds;
    private final Date since;
    private final long timeout;
    private final Presence joinPresence;

    MucEnterConfiguration(Builder builder) {
        nickname = builder.nickname;
        password = builder.password;
        maxChars = builder.maxChars;
        maxStanzas = builder.maxStanzas;
        seconds = builder.seconds;
        since = builder.since;
        timeout = builder.timeout;

        if (builder.joinPresence == null) {
            joinPresence = new Presence(Presence.Type.available);
        }
        else {
            joinPresence = builder.joinPresence.clone();
        }
        // Indicate the the client supports MUC
        joinPresence.addExtension(new MUCInitialPresence(password, maxChars, maxStanzas, seconds,
                        since));
    }

    Presence getJoinPresence(MultiUserChat multiUserChat) {
        final EntityFullJid jid = JidCreate.fullFrom(multiUserChat.getRoom(), nickname);
        joinPresence.setTo(jid);
        return joinPresence;
    }

    long getTimeout() {
        return timeout;
    }

    public static final class Builder {
        private final Resourcepart nickname;

        private String password;
        private int maxChars = -1;
        private int maxStanzas = -1;
        private int seconds = -1;
        private Date since;
        private long timeout;
        private Presence joinPresence;

        Builder(Resourcepart nickname, long timeout) {
            this.nickname = Objects.requireNonNull(nickname, "Nickname must not be null");
            timeoutAfter(timeout);
        }

        /**
         * Set the presence used to join the MUC room.
         * <p>
         * The 'to' value of the given presence will be overridden and the given presence must be of type
         * 'available', otherwise an {@link IllegalArgumentException} will be thrown.
         * <p>
         *
         * @param presence
         * @return a reference to this builder.
         */
        public Builder withPresence(Presence presence) {
            if (presence.getType() != Presence.Type.available) {
                throw new IllegalArgumentException("Presence must be of type 'available'");
            }

            joinPresence = presence;
            return this;
        }

        /**
         * Use the given password to join the MUC room.
         *
         * @param password the password used to join.
         * @return a reference to this builder.
         */
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Set the timeout used when joining the MUC room.
         *
         * @param timeout the timeout to use when joining.
         * @return a reference to this builder.
         */
        public Builder timeoutAfter(long timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Request that that MUC is going to sent us no history when joining.
         * 
         * @return a reference to this builder.
         */
        public Builder requestNoHistory() {
            maxChars = 0;
            maxStanzas = -1;
            seconds = -1;
            since = null;
            return this;
        }

        /**
         * Sets the total number of characters to receive in the history.
         * 
         * @param maxChars the total number of characters to receive in the history.
         * @return a reference to this builder.
         */
        public Builder requestMaxCharsHistory(int maxChars) {
            this.maxChars = maxChars;
            return this;
        }

        /**
         * Sets the total number of messages to receive in the history.
         * 
         * @param maxStanzas the total number of messages to receive in the history.
         * @return a reference to this builder.
         */
        public Builder requestMaxStanzasHistory(int maxStanzas) {
            this.maxStanzas = maxStanzas;
            return this;
        }

        /**
         * Sets the number of seconds to use to filter the messages received during that time. 
         * In other words, only the messages received in the last "X" seconds will be included in 
         * the history.
         * 
         * @param seconds the number of seconds to use to filter the messages received during 
         * that time.
         * @return a reference to this builder.
         */
        public Builder requestHistorySince(int seconds) {
            this.seconds = seconds;
            return this;
        }

        /**
         * Sets the since date to use to filter the messages received during that time. 
         * In other words, only the messages received since the datetime specified will be 
         * included in the history.
         * 
         * @param since the since date to use to filter the messages received during that time.
         * @return a reference to this builder.
         */
        public Builder requestHistorySince(Date since) {
            this.since = since;
            return this;
        }

        /**
         * Build a new {@link MucEnterConfiguration} with the current builder.
         *
         * @return a new {@code MucEnterConfiguration}.
         */
        public MucEnterConfiguration build() {
            return new MucEnterConfiguration(this);
        }

    }
}
