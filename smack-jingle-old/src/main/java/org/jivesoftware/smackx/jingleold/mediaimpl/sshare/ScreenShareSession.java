/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.mediaimpl.sshare;

import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api.ImageDecoder;
import org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api.ImageEncoder;
import org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api.ImageReceiver;
import org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api.ImageTransmitter;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

/**
 * This Class implements a complete JingleMediaSession.
 * It sould be used to transmit and receive captured images from the Display.
 * This Class should be automaticly controlled by JingleSession.
 * For better NAT Traversal support this implementation don't support only receive or only transmit.
 * To receive you MUST transmit. So the only implemented and functionally methods are startTransmit() and stopTransmit()
 *
 * @author Thiago Camargo
 */
public class ScreenShareSession extends JingleMediaSession {

    private static final Logger LOGGER = Logger.getLogger(ScreenShareSession.class.getName());

    private ImageTransmitter transmitter = null;
    private ImageReceiver receiver = null;
    private int width = 600;
    private int height = 600;

    /**
     * Creates a org.jivesoftware.jingleaudio.jmf.AudioMediaSession with defined payload type, remote and local candidates.
     *
     * @param payloadType Payload of the jmf
     * @param remote      the remote information. The candidate that the jmf will be sent to.
     * @param local       the local information. The candidate that will receive the jmf
     * @param locator     media locator
     */
    public ScreenShareSession(final PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local,
            final String locator, JingleSession jingleSession) {
        super(payloadType, remote, local, "Screen", jingleSession);
        initialize();
    }

    /**
     * Initialize the screen share channels.
     */
    @Override
    public void initialize() {

        JingleSession session = getJingleSession();
        if ((session != null) && (session.getInitiator().equals(session.getConnection().getUser()))) {
            // If the initiator of the jingle session is us then we transmit a screen share.
            try {
                InetAddress remote = InetAddress.getByName(getRemote().getIp());
                transmitter = new ImageTransmitter(new DatagramSocket(getLocal().getPort()), remote, getRemote().getPort(),
                        new Rectangle(0, 0, width, height));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }

        } else {
            // Otherwise we receive a screen share.
            JFrame window = new JFrame();
            JPanel jp = new JPanel();
            window.add(jp);

            window.setLocation(0, 0);
            window.setSize(600, 600);

            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    receiver.stop();
                }
            });

            try {
                receiver = new ImageReceiver(InetAddress.getByName("0.0.0.0"), getRemote().getPort(), getLocal().getPort(), width,
                        height);
                LOGGER.fine("Receiving on:" + receiver.getLocalPort());
            } catch (UnknownHostException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }

            jp.add(receiver);
            receiver.setVisible(true);
            window.setAlwaysOnTop(true);
            window.setVisible(true);
        }
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    @Override
    public void startTrasmit() {
        new Thread(transmitter).start();
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    @Override
    public void setTrasmit(boolean active) {
        transmitter.setTransmit(true);
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    @Override
    public void startReceive() {
        // Do nothing
    }

    /**
     * Stops transmission and for NAT Traversal reasons stop receiving also.
     */
    @Override
    public void stopTrasmit() {
        if (transmitter != null) {
            transmitter.stop();
        }
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    @Override
    public void stopReceive() {
        if (receiver != null) {
            receiver.stop();
        }
    }

    /**
     * Obtain a free port we can use.
     *
     * @return A free port number.
     */
    protected int getFreePort() {
        ServerSocket ss;
        int freePort = 0;

        for (int i = 0; i < 10; i++) {
            freePort = (int) (10000 + Math.round(Math.random() * 10000));
            freePort = freePort % 2 == 0 ? freePort : freePort + 1;
            try {
                ss = new ServerSocket(freePort);
                freePort = ss.getLocalPort();
                ss.close();
                return freePort;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }
        }
        try {
            ss = new ServerSocket(0);
            freePort = ss.getLocalPort();
            ss.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        return freePort;
    }

    public void setEncoder(ImageEncoder encoder) {
        if (encoder != null) {
            this.transmitter.setEncoder(encoder);
        }
    }

    public void setDecoder(ImageDecoder decoder) {
        if (decoder != null) {
            this.receiver.setDecoder(decoder);
        }
    }
}
