/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */
package org.jivesoftware.smackx.workgroup;

/**
 * An interface which all classes interested in hearing about group chat invitations should
 *  implement.
 *
 * @author loki der quaeler
 */
public interface InvitationListener {

    /**
     * The implementing class instance will be notified via this method when an invitation
     *  to join a group chat has been received from the server.
     *
     * @param invitation an Invitation instance embodying the information pertaining to the
     *                      invitation
     */
    public void invitationReceived(Invitation invitation);

}
