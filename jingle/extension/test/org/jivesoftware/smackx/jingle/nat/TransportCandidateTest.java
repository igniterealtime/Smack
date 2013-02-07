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
package org.jivesoftware.smackx.jingle.nat;

import java.util.ArrayList;
import java.util.Collections;

import org.jivesoftware.smack.test.SmackTestCase;

public class TransportCandidateTest extends SmackTestCase {

    public TransportCandidateTest(final String arg0) {
        super(arg0);
    }

    /**
     * Test for equals()
     */
    public void testEqualsObject() {
        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3468, "username", 25, ICECandidate.Type.prflx);
        TransportCandidate cand2 = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3468, "username", 25, ICECandidate.Type.prflx);
        TransportCandidate cand3 = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3469, "username", 25, ICECandidate.Type.prflx);

        assertEquals(cand1, cand2);
        assertFalse(cand1.equals(cand3));
    }

    /**
     * Test for compareTo()
     */
    public void testCompareTo() {
        int highestPref = 100;

        ICECandidate cand1 = new ICECandidate("192.168.2.1", 3, 2,
                "password", 3468, "username", 1, ICECandidate.Type.prflx);
        ICECandidate cand2 = new ICECandidate("192.168.5.1", 2, 10,
                "password", 3469, "username", 15,ICECandidate.Type.prflx);
        ICECandidate candH = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3468, "username", highestPref, ICECandidate.Type.prflx);
        ICECandidate cand3 = new ICECandidate("192.168.2.10", 2, 10,
                "password", 3469, "username", 2, ICECandidate.Type.prflx);
        ICECandidate cand4 = new ICECandidate("192.168.4.1", 3, 2,
                "password", 3468, "username", 78, ICECandidate.Type.prflx);

        ArrayList<ICECandidate> candList = new ArrayList<ICECandidate>();
        candList.add(cand1);
        candList.add(cand2);
        candList.add(candH);
        candList.add(cand3);
        candList.add(cand4);

        Collections.sort(candList);
        assertEquals(candList.get(candList.size() - 1), candH);
    }

    protected int getMaxConnections() {
        return 0;
    }
}
