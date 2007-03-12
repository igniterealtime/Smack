package org.jivesoftware.smackx.jingle.nat;

import java.util.ArrayList;
import java.util.Collections;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetAddress;

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
                "password", 3468, "username", 25, "");
        TransportCandidate cand2 = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3468, "username", 25, "");
        TransportCandidate cand3 = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3469, "username", 25, "");

        assertEquals(cand1, cand2);
        assertFalse(cand1.equals(cand3));
    }

    /**
     * Test for compareTo()
     */
    public void testCompareTo() {
        int highestPref = 100;

        TransportCandidate cand1 = new ICECandidate("192.168.2.1", 3, 2,
                "password", 3468, "username", 1, "");
        TransportCandidate cand2 = new ICECandidate("192.168.5.1", 2, 10,
                "password", 3469, "username", 15, "");
        TransportCandidate candH = new ICECandidate("192.168.2.1", 1, 2,
                "password", 3468, "username", highestPref, "");
        TransportCandidate cand3 = new ICECandidate("192.168.2.10", 2, 10,
                "password", 3469, "username", 2, "");
        TransportCandidate cand4 = new ICECandidate("192.168.4.1", 3, 2,
                "password", 3468, "username", 78, "");

        ArrayList candList = new ArrayList();
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
