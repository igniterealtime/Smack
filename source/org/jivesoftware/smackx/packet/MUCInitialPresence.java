/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
package org.jivesoftware.smackx.packet;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents extended presence information whose sole purpose is to signal the ability of 
 * the occupant to speak the MUC protocol when joining a room. If the room requires a password 
 * then the MUCInitialPresence should include one.<p>
 * 
 * The amount of discussion history provided on entering a room (perhaps because the 
 * user is on a low-bandwidth connection or is using a small-footprint client) could be managed by
 * setting a configured History instance to the MUCInitialPresence instance. 
 * @see MUCInitialPresence#setHistory(MUCInitialPresence.History).
 *
 * @author Gaston Dombiak
 */
public class MUCInitialPresence implements PacketExtension {

    private String password;
    private History history; 

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "http://jabber.org/protocol/muc";
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        if (getPassword() != null) {
            buf.append("<password>").append(getPassword()).append("</password>");
        }
        if (getHistory() != null) {
            buf.append(getHistory().toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
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
     */
    public void setHistory(History history) {
        this.history = history;
    }

    /**
     * Sets the password to use when the room requires a password.
     * 
     * @param password the password to use when the room requires a password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * The History class controls the number of characters or messages to receive
     * when entering a room.
     * 
     * @author Gaston Dombiak
     */
    public static class History {

        private int maxChars = -1;
        private int maxStanzas = -1; 
        private int seconds = -1; 
        private Date since; 

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
         */
        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        /**
         * Sets the total number of messages to receive in the history.
         * 
         * @param maxStanzas the total number of messages to receive in the history.
         */
        public void setMaxStanzas(int maxStanzas) {
            this.maxStanzas = maxStanzas;
        }

        /**
         * Sets the number of seconds to use to filter the messages received during that time. 
         * In other words, only the messages received in the last "X" seconds will be included in 
         * the history.
         * 
         * @param seconds he number of seconds to use to filter the messages received during 
         * that time.
         */
        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

        /**
         * Sets the since date to use to filter the messages received during that time. 
         * In other words, only the messages received since the datetime specified will be 
         * included in the history.
         * 
         * @param since the since date to use to filter the messages received during that time.
         */
        public void setSince(Date since) {
            this.since = since;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<history");
            if (getMaxChars() != -1) {
                buf.append(" maxchars=\"").append(getMaxChars()).append("\"");
            }
            if (getMaxStanzas() != -1) {
                buf.append(" maxstanzas=\"").append(getMaxStanzas()).append("\"");
            }
            if (getSeconds() != -1) {
                buf.append(" seconds=\"").append(getSeconds()).append("\"");
            }
            if (getSince() != null) {
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'hh:mm:ss");
                buf.append(" seconds=\"").append(utcFormat.format(getSince())).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }
    }
}
