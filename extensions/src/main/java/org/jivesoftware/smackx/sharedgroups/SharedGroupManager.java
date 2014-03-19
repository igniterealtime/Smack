/**
 *
 * Copyright 2005 Jive Software.
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
package org.jivesoftware.smackx.sharedgroups;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.sharedgroups.packet.SharedGroupsInfo;

import java.util.List;

/**
 * A SharedGroupManager provides services for discovering the shared groups where a user belongs.<p>
 *
 * Important note: This functionality is not part of the XMPP spec and it will only work
 * with Wildfire.
 *
 * @author Gaston Dombiak
 */
public class SharedGroupManager {

    /**
     * Returns the collection that will contain the name of the shared groups where the user
     * logged in with the specified session belongs.
     *
     * @param connection connection to use to get the user's shared groups.
     * @return collection with the shared groups' name of the logged user.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     */
    public static List<String> getSharedGroups(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException {
        // Discover the shared groups of the logged user
        SharedGroupsInfo info = new SharedGroupsInfo();
        info.setType(IQ.Type.GET);

        SharedGroupsInfo result = (SharedGroupsInfo) connection.createPacketCollectorAndSend(info).nextResultOrThrow();
        return result.getGroups();
    }
}
