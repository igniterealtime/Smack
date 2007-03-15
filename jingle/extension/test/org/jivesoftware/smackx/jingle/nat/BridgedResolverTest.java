package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.jingle.IncomingJingleSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionRequest;
import org.jivesoftware.smackx.jingle.OutgoingJingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BridgedResolverTest extends SmackTestCase {

    private int counter;

    private final Object mutex = new Object();

    public BridgedResolverTest(String arg) {
        super(arg);
    }

    // Counter management

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    private void incCounter() {
        synchronized (mutex) {
            counter++;
        }
    }

    private int valCounter() {
        int val;
        synchronized (mutex) {
            val = counter;
        }
        return val;
    }

    public void testCheckService() {
        assertTrue(RTPBridge.serviceAvailable(getConnection(0)));
    }

    public void testGetBridge() {

        resetCounter();

        RTPBridge rtpBridge = RTPBridge.getRTPBridge(getConnection(0), "001");

        System.out.println(rtpBridge.getIp() + " portA:" + rtpBridge.getPortA() + " portB:" + rtpBridge.getPortB());

        if (rtpBridge != null) {
            if (rtpBridge.getIp() != null) incCounter();
            if (rtpBridge.getPortA() != -1) incCounter();
            if (rtpBridge.getPortB() != -1) incCounter();
        }

        assertTrue(valCounter() == 3);
    }

    public void testGetPublicIp() {

        resetCounter();

        String publicIp = RTPBridge.getPublicIP(getConnection(0));

        System.out.println(publicIp + " local:" + getConnection(0).getLocalNetworkAddress().getHostAddress());

        if (publicIp != null) {
            incCounter();
        }

        try {
            InetAddress localaddr = InetAddress.getLocalHost();
            System.out.println("main Local IP Address : " + localaddr.getHostAddress());
            System.out.println("main Local hostname   : " + localaddr.getHostName());

            InetAddress[] localaddrs = InetAddress.getAllByName("localhost");
            for (int i = 0; i < localaddrs.length; i++) {
                if (!localaddrs[i].equals(localaddr)) {
                    System.out.println("alt  Local IP Address : " + localaddrs[i].getHostAddress());
                    System.out.println("alt  Local hostname   : " + localaddrs[i].getHostName());
                    System.out.println();
                }
            }
        }
        catch (UnknownHostException e) {
            System.err.println("Can't detect localhost : " + e);
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(valCounter() == 1);
    }

    protected int getMaxConnections() {
        return 1;
    }

}
