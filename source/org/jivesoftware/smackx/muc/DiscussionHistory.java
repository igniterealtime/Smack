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
package org.jivesoftware.smackx.muc;

import java.util.Date;

import org.jivesoftware.smackx.packet.MUCInitialPresence;

/**
 * The DiscussionHistory class controls the number of characters or messages to receive
 * when entering a room. The room will decide the amount of history to return if you don't 
 * specify a DiscussionHistory while joining a room.<p>
 * 
 * You can use some or all of these variable to control the amount of history to receive:   
 * <ul>
 *  <li>maxchars -> total number of characters to receive in the history.
 *  <li>maxstanzas -> total number of messages to receive in the history.
 *  <li>seconds -> only the messages received in the last "X" seconds will be included in the 
 * history.
 *  <li>since -> only the messages received since the datetime specified will be included in 
 * the history.
 * </ul>
 * 
 * Note: Setting maxchars to 0 indicates that the user requests to receive no history.
 * 
 * @author Gaston Dombiak
 */
public class DiscussionHistory {

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
     * @param seconds the number of seconds to use to filter the messages received during 
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

    /**
     * Returns true if the history has been configured with some values.
     * 
     * @return true if the history has been configured with some values.
     */
    private boolean isConfigured() {
        return maxChars > -1 || maxStanzas > -1 || seconds > -1 || since != null;
    }

    /**
     * Returns the History that manages the amount of discussion history provided on entering a 
     * room.
     * 
     * @return the History that manages the amount of discussion history provided on entering a 
     * room.
     */
    MUCInitialPresence.History getMUCHistory() {
        // Return null if the history was not properly configured  
        if (!isConfigured()) {
            return null;
        }
        
        MUCInitialPresence.History mucHistory = new MUCInitialPresence.History();
        if (maxChars > -1) {
            mucHistory.setMaxChars(maxChars);
        }
        if (maxStanzas > -1) {
            mucHistory.setMaxStanzas(maxStanzas);
        }
        if (seconds > -1) {
            mucHistory.setSeconds(seconds);
        }
        if (since != null) {
            mucHistory.setSince(since);
        }
        return mucHistory;
    }
}
