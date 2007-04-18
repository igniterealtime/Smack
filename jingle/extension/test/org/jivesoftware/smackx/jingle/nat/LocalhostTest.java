package org.jivesoftware.smackx.jingle.nat;

import junit.framework.TestCase;

import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.DatagramSocket;

public class LocalhostTest extends TestCase {

    public void testGetLocalhost() {
        System.out.println(BridgedResolver.getLocalHost());
    }

}
