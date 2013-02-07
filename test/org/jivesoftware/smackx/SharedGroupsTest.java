/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
package org.jivesoftware.smackx;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPException;

import java.util.List;

/**
 * Test cases for getting the shared groups of a user.<p>
 *
 * Important note: This functionality is not part of the XMPP spec and it will only work
 * with Wildfire.
 *
 * @author Gaston Dombiak
 */
public class SharedGroupsTest extends SmackTestCase {

    public SharedGroupsTest(String arg0) {
        super(arg0);
    }

    public void testGetUserSharedGroups() throws XMPPException {
        List<String> groups = SharedGroupManager.getSharedGroups(getConnection(0));

        assertNotNull("User groups was null", groups);
    }

    protected int getMaxConnections() {
        return 1;
    }
}
