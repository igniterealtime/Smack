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
 * Requests made by users to get support by some agent.
 *
 * @author Gaston Dombiak
 */
public class UserRequest extends OfferContent {
    // TODO Do we want to use a singleton? Should we store the userID here?
    private static UserRequest instance = new UserRequest();

    public static OfferContent getInstance() {
        return instance;
    }

    boolean isUserRequest() {
        return true;
    }

    boolean isInvitation() {
        return false;
    }

    boolean isTransfer() {
        return false;
    }

}
