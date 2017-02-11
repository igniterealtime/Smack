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
package org.jivesoftware.smackx.jingleold.mediaimpl.jmf;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.MediaLocator;

import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

/**
 * This Class implements a complete JingleMediaSession.
 * It sould be used to transmit and receive audio captured from the Mic.
 * This Class should be automaticly controlled by JingleSession.
 * But you could also use in any VOIP application.
 * For better NAT Traversal support this implementation don't support only receive or only transmit.
 * To receive you MUST transmit. So the only implemented and functionally methods are startTransmit() and stopTransmit()
 *
 * @author Thiago Camargo
 */
public class AudioMediaSession extends JingleMediaSession {

    private static final Logger LOGGER = Logger.getLogger(AudioMediaSession.class.getName());

    private AudioChannel audioChannel;

    /**
     * Creates a org.jivesoftware.jingleaudio.jmf.AudioMediaSession with defined payload type, remote and local candidates.
     *
     * @param payloadType Payload of the jmf
     * @param remote      the remote information. The candidate that the jmf will be sent to.
     * @param local       the local information. The candidate that will receive the jmf
     * @param locator     media locator
     */
    public AudioMediaSession(final PayloadType payloadType, final TransportCandidate remote,
            final TransportCandidate local, String locator, JingleSession jingleSession) {
        super(payloadType, remote, local, locator==null?"dsound://":locator,jingleSession);
        initialize();
    }

    /**
     * Initialize the Audio Channel to make it able to send and receive audio.
     */
    @Override
    public void initialize() {

        String ip;
        String localIp;
        int localPort;
        int remotePort;

        if (this.getLocal().getSymmetric() != null) {
            ip = this.getLocal().getIp();
            localIp = this.getLocal().getLocalIp();
            localPort = getFreePort();
            remotePort = this.getLocal().getSymmetric().getPort();

            LOGGER.fine(this.getLocal().getConnection() + " " + ip + ": " + localPort + "->" + remotePort);

        }
        else {
            ip = this.getRemote().getIp();
            localIp = this.getLocal().getLocalIp();
            localPort = this.getLocal().getPort();
            remotePort = this.getRemote().getPort();
        }

        audioChannel = new AudioChannel(new MediaLocator(this.getMediaLocator()), localIp, ip, localPort, remotePort, AudioFormatUtils.getAudioFormat(this.getPayloadType()),this);
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    @Override
    public void startTrasmit() {
        audioChannel.start();
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    @Override
    public void setTrasmit(boolean active) {
        audioChannel.setTrasmit(active);
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
        if (audioChannel != null)
            audioChannel.stop();
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    @Override
    public void stopReceive() {
        // Do nothing
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
            }
            catch (IOException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }
        }
        try {
            ss = new ServerSocket(0);
            freePort = ss.getLocalPort();
            ss.close();
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        return freePort;
    }

}
