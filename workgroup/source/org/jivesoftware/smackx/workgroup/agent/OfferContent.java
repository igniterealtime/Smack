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

package org.jivesoftware.smackx.workgroup.agent;

/**
 * Type of content being included in the offer. The content actually explains the reason
 * the agent is getting an offer.
 *
 * @author Gaston Dombiak
 */
public abstract class OfferContent {

    /**
     * Returns true if the content of the offer is related to a user request. This is the
     * most common type of offers an agent should receive.
     *
     * @return true if the content of the offer is related to a user request.
     */
    abstract boolean isUserRequest();

    /**
     * Returns true if the content of the offer is related to a room invitation made by another
     * agent. This type of offer include the room to join, metadata sent by the user while joining
     * the queue and the reason why the agent is being invited.
     *
     * @return true if the content of the offer is related to a room invitation made by another agent.
     */
    abstract boolean isInvitation();

    /**
     * Returns true if the content of the offer is related to a service transfer made by another
     * agent. This type of offers include the room to join, metadata sent by the user while joining the
     * queue and the reason why the agent is receiving the transfer offer.
     *
     * @return true if the content of the offer is related to a service transfer made by another agent.
     */
    abstract boolean isTransfer();
}
