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

    protected int getMaxConnections() {
        return 1;
    }

}
