/**
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

package org.jivesoftware.smackx.workgroup;

/**
 * An interface which all classes interested in hearing about group chat invitations should
 *  implement.
 *
 * @author loki der quaeler
 */
public interface WorkgroupInvitationListener {

    /**
     * The implementing class instance will be notified via this method when an invitation
     *  to join a group chat has been received from the server.
     *
     * @param invitation an Invitation instance embodying the information pertaining to the
     *                      invitation
     */
    public void invitationReceived(WorkgroupInvitation invitation);

}
