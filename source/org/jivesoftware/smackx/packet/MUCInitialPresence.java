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
 * Represents...
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
            buf.append(" password=\"").append(getPassword()).append("\"");
        }
        if (getHistory() != null) {
            buf.append(getHistory().toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * @return
     */
    public History getHistory() {
        return history;
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param history
     */
    public void setHistory(History history) {
        this.history = history;
    }

    /**
     * @param string
     */
    public void setPassword(String string) {
        password = string;
    }

    public static class History {

        private int maxChars = -1;
        private int maxStanzas = -1; 
        private int seconds = -1; 
        private Date since; 

        /**
         * @return
         */
        public int getMaxChars() {
            return maxChars;
        }

        /**
         * @return
         */
        public int getMaxStanzas() {
            return maxStanzas;
        }

        /**
         * @return
         */
        public int getSeconds() {
            return seconds;
        }

        /**
         * @return
         */
        public Date getSince() {
            return since;
        }

        /**
         * @param i
         */
        public void setMaxChars(int i) {
            maxChars = i;
        }

        /**
         * @param i
         */
        public void setMaxStanzas(int i) {
            maxStanzas = i;
        }

        /**
         * @param i
         */
        public void setSeconds(int i) {
            seconds = i;
        }

        /**
         * @param date
         */
        public void setSince(Date date) {
            since = date;
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
