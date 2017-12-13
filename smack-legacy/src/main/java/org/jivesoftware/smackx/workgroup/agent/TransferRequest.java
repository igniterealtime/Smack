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

package org.jivesoftware.smackx.workgroup.agent;

/**
 * Request sent by an agent to transfer a support session to another agent or user.
 *
 * @author Gaston Dombiak
 */
public class TransferRequest extends OfferContent {

    private final String inviter;
    private final String room;
    private final String reason;

    public TransferRequest(String inviter, String room, String reason) {
        this.inviter = inviter;
        this.room = room;
        this.reason = reason;
    }

    public String getInviter() {
        return inviter;
    }

    public String getRoom() {
        return room;
    }

    public String getReason() {
        return reason;
    }

    @Override
    boolean isUserRequest() {
        return false;
    }

    @Override
    boolean isInvitation() {
        return false;
    }

    @Override
    boolean isTransfer() {
        return true;
    }
}
