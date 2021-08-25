/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;

import org.junit.Test;

public class JingleTransportComparatorTest extends SmackTestSuite {

    @Test
    public void comparisonTest() {
        DummyConnection dummyConnection = new DummyConnection();
        JingleIBBTransportManager loser = JingleIBBTransportManager.getInstanceFor(dummyConnection);
        JingleS5BTransportManager winner = JingleS5BTransportManager.getInstanceFor(dummyConnection);

        assertEquals(-1, loser.compareTo(winner));
        assertEquals(1, winner.compareTo(loser));
        assertEquals(-1, loser.getPriority()); // IBB should always be last resort.
    }
}
