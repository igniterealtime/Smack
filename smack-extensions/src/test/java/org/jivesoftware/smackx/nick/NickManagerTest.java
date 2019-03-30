/**
 *
 * Copyright Miguel Hincapie
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
package org.jivesoftware.smackx.nick;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smackx.InitExtensions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class NickManagerTest extends InitExtensions {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void getInstanceFor() throws Exception {
        DummyConnection dummyConnection = new DummyConnection();
        dummyConnection.connect();
        NickManager nickManager = NickManager.getInstanceFor(dummyConnection);
        assertNotNull(nickManager);
    }

    @Test
    public void addNickMessageListener() {
    }

    @Test
    public void removeNickMessageListener() {
    }

    @Test
    public void sendNickMessage() {
    }
}
