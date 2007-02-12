/**
 * $RCSfile$
/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
