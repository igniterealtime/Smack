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

    public void testFullBridge() {
        resetCounter();

        try {

            //XMPPConnection.DEBUG_ENABLED = true;

            XMPPConnection x0 = new XMPPConnection("thiago");
            XMPPConnection x1 = new XMPPConnection("thiago");

            x0.connect();
            x0.login("barata7", "barata7");
            x1.connect();
            x1.login("barata6", "barata6");

            final JingleManager jm0 = new JingleManager(
                    x0, new BridgedResolver(x0));
            final JingleManager jm1 = new JingleManager(
                    x1, new BridgedResolver(x1));

            JingleMediaManager jingleMediaManager = new JingleMediaManager() {
                // Media Session Implementation
                public JingleMediaSession createMediaSession(final PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local) {
                    return new JingleMediaSession(payloadType, remote, local) {

                        public void initialize() {

                        }

                        public void startTrasmit() {
                            incCounter();

                            System.out.print("IPs:");
                            System.out.println(local.getSymmetric().getIp());
                            System.out.println(local.getIp());

                            System.out.println("Transmit");
                        }

                        public void startReceive() {
                            incCounter();
                            System.out.println("Receive");
                        }

                        public void setTrasmit(boolean active) {
                        }

                        public void stopTrasmit() {
                            incCounter();
                            System.out.println("Stop Transmit");
                        }

                        public void stopReceive() {
                            incCounter();
                            System.out.println("Stop Receive");
                        }
                    };
                }
            };

            jingleMediaManager.addPayloadType(new PayloadType.Audio(3, "GSM", 1, 16000));

            jm0.setMediaManager(jingleMediaManager);
            jm1.setMediaManager(jingleMediaManager);

            jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
                public void sessionRequested(final JingleSessionRequest request) {

                    try {

                        IncomingJingleSession session = request.accept(jm1.getMediaManager().getPayloads());

                        session.start(request);
                    } catch (XMPPException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                }
            });

            OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("barata6@thiago/Smack");

            js0.start();

            Thread.sleep(10000);
            js0.terminate();

            Thread.sleep(3000);

            System.out.println(valCounter());

            assertTrue(valCounter() == 8);
            //Thread.sleep(15000);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected int getMaxConnections() {
        return 1;
    }

}
