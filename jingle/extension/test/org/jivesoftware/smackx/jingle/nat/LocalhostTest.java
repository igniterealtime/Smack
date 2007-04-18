package org.jivesoftware.smackx.jingle.nat;

import junit.framework.TestCase;

public class LocalhostTest extends TestCase {

    public void testGetLocalhost() {
        System.out.println(BridgedResolver.getLocalHost());
    }

}
