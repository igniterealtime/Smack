/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smackx.bookmark;

/**
 *  Interface to indicate if a bookmark is shared across the server.
 *
 * @author Alexander Wenckus
 */
public interface SharedBookmark {

    /**
     * Returns true if this bookmark is shared.
     *
     * @return returns true if this bookmark is shared.
     */
    public boolean isShared();
}
