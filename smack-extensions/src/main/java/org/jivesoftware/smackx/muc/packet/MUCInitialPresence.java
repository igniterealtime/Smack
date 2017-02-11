/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.muc.packet;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.util.XmppDateTime;

import java.util.Date;

/**
 * Represents extended presence information whose sole purpose is to signal the ability of 
 * the occupant to speak the MUC protocol when joining a room. If the room requires a password 
 * then the MUCInitialPresence should include one.
 * <p>
 * The amount of discussion history provided on entering a room (perhaps because the 
 * user is on a low-bandwidth connection or is using a small-footprint client) could be managed by
 * setting a configured History instance to the MUCInitialPresence instance. 
 *
 * @author Gaston Dombiak
 * @see MUCInitialPresence#setHistory(MUCInitialPresence.History)
 */
public class MUCInitialPresence implements ExtensionElement {

    public static final String ELEMENT = "x";
    public static final String NAMESPACE = "http://jabber.org/protocol/muc";

    // TODO make those fields final once deprecated setter methods have been removed.
    private String password;
    private History history; 

    /**
     * Deprecated constructor.
     * @deprecated use {@link #MUCInitialPresence(String, int, int, int, Date)} instead.
     */
    @Deprecated
    public MUCInitialPresence() {
    }

    /**
     * Construct a new MUC initial presence extension.
     *
     * @param password the optional password used to enter the room.
     * @param maxChars the maximal count of characters of history to request.
     * @param maxStanzas the maximal count of stanzas of history to request.
     * @param seconds the last seconds since when to request history.
     * @param since the date since when to request history.
     */
    public MUCInitialPresence(String password, int maxChars, int maxStanzas, int seconds, Date since) {
        this.password = password;
        if (maxChars > -1 || maxStanzas > -1 || seconds > -1 || since != null) {
            this.history = new History(maxChars, maxStanzas, seconds, since);
        } else {
            this.history = null;
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.optElement("password", getPassword());
        xml.optElement(getHistory());
        xml.closeElement(this);
        return xml;
    }

    /**
     * Returns the history that manages the amount of discussion history provided on 
     * entering a room.
     * 
     * @return the history that manages the amount of discussion history provided on 
     * entering a room.
     */
    public History getHistory() {
        return history;
    }

    /**
     * Returns the password to use when the room requires a password.
     * 
     * @return the password to use when the room requires a password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the History that manages the amount of discussion history provided on 
     * entering a room.
     * 
     * @param history that manages the amount of discussion history provided on 
     * entering a room.
     * @deprecated use {@link #MUCInitialPresence(String, int, int, int, Date)} instead.
     */
    @Deprecated
    public void setHistory(History history) {
        this.history = history;
    }

    /**
     * Sets the password to use when the room requires a password.
     * 
     * @param password the password to use when the room requires a password.
     * @deprecated use {@link #MUCInitialPresence(String, int, int, int, Date)} instead.
     */
    @Deprecated
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retrieve the MUCInitialPresence PacketExtension from packet, if any.
     *
     * @param packet
     * @return the MUCInitialPresence PacketExtension or {@code null}
     * @deprecated use {@link #from(Stanza)} instead
     */
    @Deprecated
    public static MUCInitialPresence getFrom(Stanza packet) {
        return from(packet);
    }

    /**
     * Retrieve the MUCInitialPresence PacketExtension from packet, if any.
     *
     * @param packet
     * @return the MUCInitialPresence PacketExtension or {@code null}
     */
    public static MUCInitialPresence from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }

    /**
     * The History class controls the number of characters or messages to receive
     * when entering a room.
     * 
     * @author Gaston Dombiak
     */
    public static class History implements NamedElement {

        public static final String ELEMENT = "history";

        // TODO make those fields final once the deprecated setter methods have been removed.
        private int maxChars;
        private int maxStanzas;
        private int seconds;
        private Date since; 

        /**
         * Deprecated constructor.
         * @deprecated use {@link #MUCInitialPresence.History(int, int, int, Date)} instead.
         */
        @Deprecated
        public History() {
            this.maxChars = -1;
            this.maxStanzas = -1;
            this.seconds = -1;
        }

        public History(int maxChars, int maxStanzas, int seconds, Date since) {
            if (maxChars < 0 && maxStanzas < 0 && seconds < 0 && since == null) {
                throw new IllegalArgumentException();
            }
            this.maxChars = maxChars;
            this.maxStanzas = maxStanzas;
            this.seconds = seconds;
            this.since = since;
        }

        /**
         * Returns the total number of characters to receive in the history.
         * 
         * @return total number of characters to receive in the history.
         */
        public int getMaxChars() {
            return maxChars;
        }

        /**
         * Returns the total number of messages to receive in the history.
         * 
         * @return the total number of messages to receive in the history.
         */
        public int getMaxStanzas() {
            return maxStanzas;
        }

        /**
         * Returns the number of seconds to use to filter the messages received during that time. 
         * In other words, only the messages received in the last "X" seconds will be included in 
         * the history.
         * 
         * @return the number of seconds to use to filter the messages received during that time.
         */
        public int getSeconds() {
            return seconds;
        }

        /**
         * Returns the since date to use to filter the messages received during that time. 
         * In other words, only the messages received since the datetime specified will be 
         * included in the history.
         * 
         * @return the since date to use to filter the messages received during that time.
         */
        public Date getSince() {
            return since;
        }

        /**
         * Sets the total number of characters to receive in the history.
         * 
         * @param maxChars the total number of characters to receive in the history.
         * @deprecated use {@link #MUCInitialPresence.History(int, int, int, Date)} instead.
         */
        @Deprecated
        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        /**
         * Sets the total number of messages to receive in the history.
         * 
         * @param maxStanzas the total number of messages to receive in the history.
         * @deprecated use {@link #MUCInitialPresence.History(int, int, int, Date)} instead.
         */
        @Deprecated
        public void setMaxStanzas(int maxStanzas) {
            this.maxStanzas = maxStanzas;
        }

        /**
         * Sets the number of seconds to use to filter the messages received during that time. 
         * In other words, only the messages received in the last "X" seconds will be included in 
         * the history.
         * 
         * @param seconds the number of seconds to use to filter the messages received during 
         * that time.
         * @deprecated use {@link #MUCInitialPresence.History(int, int, int, Date)} instead.
         */
        @Deprecated
        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

        /**
         * Sets the since date to use to filter the messages received during that time. 
         * In other words, only the messages received since the datetime specified will be 
         * included in the history.
         * 
         * @param since the since date to use to filter the messages received during that time.
         * @deprecated use {@link #MUCInitialPresence.History(int, int, int, Date)} instead.
         */
        @Deprecated
        public void setSince(Date since) {
            this.since = since;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optIntAttribute("maxchars", getMaxChars());
            xml.optIntAttribute("maxstanzas", getMaxStanzas());
            xml.optIntAttribute("seconds", getSeconds());
            if (getSince() != null) {
                xml.attribute("since", XmppDateTime.formatXEP0082Date(getSince()));
            }
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
