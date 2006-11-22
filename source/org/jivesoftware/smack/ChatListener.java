/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smack;

/**
 * A listener for chat related events.
 *
 * @author Alexander Wenckus
 */
public interface ChatListener {

    /**
     * Event fired when a new chat is created.
     *
     * @param chat the chat that was created.
     * @param createdLocally true if the chat was created by the local user and false if it wasn't.
     */
    void chatCreated(Chat chat, boolean createdLocally);
}
