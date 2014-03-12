/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the behavior of the roster if the connection is not authenticated yet.
 * 
 * @author Henning Staib
 */
public class RosterOfflineTest {

    XMPPConnection connection;

    Roster roster;

    @Before
    public void setup() throws XMPPException, SmackException {
        this.connection = new TCPConnection("localhost");
        assertFalse(connection.isConnected());

        roster = connection.getRoster();
        assertNotNull(roster);
    }

    @Test(expected = SmackException.class)
    public void shouldThrowExceptionOnCreateEntry() throws Exception {
        roster.createEntry("test", "test", null);
    }

    @Test(expected = SmackException.class)
    public void shouldThrowExceptionOnCreateGroup() throws Exception {
        roster.createGroup("test");
    }

    @Test(expected = SmackException.class)
    public void shouldThrowExceptionOnReload() throws Exception {
        roster.reload();
    }

    @Test(expected = SmackException.class)
    public void shouldThrowExceptionRemoveEntry() throws Exception {
        roster.removeEntry(null);
    }

}
