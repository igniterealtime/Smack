package org.jivesoftware.jingleaudio.jspeex;

import mil.jfcom.cie.media.session.MediaSession;
import mil.jfcom.cie.media.session.MediaSessionListener;
import mil.jfcom.cie.media.session.StreamPlayer;
import mil.jfcom.cie.media.srtp.packetizer.SpeexFormat;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

import javax.media.NoProcessorException;
import javax.media.format.UnsupportedFormatException;
import javax.media.rtp.rtcp.SenderReport;
import javax.media.rtp.rtcp.SourceDescription;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;

/**
 * $RCSfile$
 * $Revision: $
 * $Date: 25/12/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class AudioMediaSession extends JingleMediaSession implements MediaSessionListener {

    private MediaSession mediaSession;

    /**
     * Create session for test program.
     *
     * @param micOn microphone turned on
     */
    public static MediaSession createSession(String localhost, int localPort, String remoteHost, int remotePort, MediaSessionListener eventHandler, int quality, boolean secure, boolean micOn) throws NoProcessorException, UnsupportedFormatException, IOException, GeneralSecurityException {

        SpeexFormat.setFramesPerPacket(1);
        /**
         * The master key. Hardcoded for now.
         */
        byte[] masterKey = new byte[]{(byte) 0xE1, (byte) 0xF9, 0x7A, 0x0D, 0x3E, 0x01, (byte) 0x8B, (byte) 0xE0, (byte) 0xD6, 0x4F, (byte) 0xA3, 0x2C, 0x06, (byte) 0xDE, 0x41, 0x39};

        /**
         * The master salt. Hardcoded for now.
         */
        byte[] masterSalt = new byte[]{0x0E, (byte) 0xC6, 0x75, (byte) 0xAD, 0x49, (byte) 0x8A, (byte) 0xFE, (byte) 0xEB, (byte) 0xB6, (byte) 0x96, 0x0B, 0x3A, (byte) 0xAB, (byte) 0xE6};

        DatagramSocket[] localPorts = MediaSession.getLocalPorts(InetAddress.getByName(localhost), localPort);
        MediaSession session = MediaSession.createInstance(remoteHost, remotePort, localPorts, quality, secure, masterKey, masterSalt);
        session.setListener(eventHandler);

        session.setSourceDescription(new SourceDescription[]{new SourceDescription(SourceDescription.SOURCE_DESC_NAME, "Superman", 1, false), new SourceDescription(SourceDescription.SOURCE_DESC_EMAIL, "cdcie.tester@je.jfcom.mil", 1, false), new SourceDescription(SourceDescription.SOURCE_DESC_LOC, InetAddress.getByName(localhost) + " Port " + session.getLocalDataPort(), 1, false), new SourceDescription(SourceDescription.SOURCE_DESC_TOOL, "JFCOM CDCIE Audio Chat", 1, false)});
        return session;
    }


    /**
     * Creates a org.jivesoftware.jingleaudio.jmf.AudioMediaSession with defined payload type, remote and local candidates
     *
     * @param payloadType Payload of the jmf
     * @param remote      The remote information. The candidate that the jmf will be sent to.
     * @param local       The local information. The candidate that will receive the jmf
     */
    public AudioMediaSession(final PayloadType payloadType, final TransportCandidate remote,
                             final TransportCandidate local) {
        super(payloadType, remote, local);
    }

    /**
     * Initialize the Audio Channel to make it able to send and receive audio
     */
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

            System.out.println(this.getLocal().getConnection() + " " + ip + ": " + localPort + "->" + remotePort);

        } else {
            ip = this.getRemote().getIp();
            localIp = this.getLocal().getLocalIp();
            localPort = this.getLocal().getPort();
            remotePort = this.getRemote().getPort();
        }

        try {
            mediaSession = createSession(localIp, localPort, ip, remotePort, this, 2, false, true);
        } catch (NoProcessorException e) {
            e.printStackTrace();
        } catch (UnsupportedFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        try {
            System.out.println("start");
            mediaSession.start(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active
     */
    public void setTrasmit(boolean active) {
        // Do nothing
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void startReceive() {
        // Do nothing
    }

    /**
     * Stops transmission and for NAT Traversal reasons stop receiving also.
     */
    public void stopTrasmit() {
        mediaSession.close();
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void stopReceive() {
        // Do nothing
    }

    public void newStreamIdentified(StreamPlayer streamPlayer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void senderReportReceived(SenderReport report) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void streamClosed(StreamPlayer stream, boolean timeout) {
        //To change body of implemented methods use File | Settings | File Templates.
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
                e.printStackTrace();
            }
        }
        try {
            ss = new ServerSocket(0);
            freePort = ss.getLocalPort();
            ss.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return freePort;
    }
}
