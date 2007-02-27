/**
 * $RCSfile$
 * $Revision: $
 * $Date: 08/11/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.jingleaudio.jmf;

import javax.media.*;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * An Easy to use Audio Channel implemented using JMF.
 * It sends and receives jmf for and from desired IPs and ports.
 * Also has a rport Symetric behavior for better NAT Traversal.
 * It send data from a defined port and receive data in the same port, making NAT binds easier.
 * <p/>
 * Send from portA to portB and receive from portB in portA.
 * <p/>
 * Sending
 * portA ---> portB
 * <p/>
 * Receiving
 * portB ---> portA
 * <p/>
 * <i>Transmit and Receive are interdependents. To receive you MUST trasmit. </i>
 *
 * @author Thiago Camargo
 */
public class AudioChannel {

    private MediaLocator locator;
    private String localIpAddress;
    private String ipAddress;
    private int localPort;
    private int portBase;
    private Format format;

    private Processor processor = null;
    private RTPManager rtpMgrs[];
    private DataSource dataOutput = null;
    private AudioReceiver audioReceiver;

    private List<SendStream> sendStreams = new ArrayList<SendStream>();

    private boolean started = false;

    /**
     * Creates an Audio Channel for a desired jmf locator. For instance: new MediaLocator("dsound://")
     *
     * @param locator
     * @param ipAddress
     * @param localPort
     * @param remotePort
     * @param format
     */
    public AudioChannel(MediaLocator locator,
                        String localIpAddress,
                        String ipAddress,
                        int localPort,
                        int remotePort,
                        Format format) {

        this.locator = locator;
        this.localIpAddress = localIpAddress;
        this.ipAddress = ipAddress;
        this.localPort = localPort;
        this.portBase = remotePort;
        this.format = format;
    }

    /**
     * Starts the transmission. Returns null if transmission started ok.
     * Otherwise it returns a string with the reason why the setup failed.
     * Starts receive also.
     */
    public synchronized String start() {
        if (started) return null;
        started = true;
        String result;

        // Create a processor for the specified jmf locator
        result = createProcessor();
        if (result != null) {
            started = false;
            return result;
        }

        // Create an RTP session to transmit the output of the
        // processor to the specified IP address and port no.
        result = createTransmitter();
        if (result != null) {
            processor.close();
            processor = null;
            started = false;
            return result;
        }

        // Start the transmission
        processor.start();

        return null;
    }

    /**
     * Stops the transmission if already started.
     * Stops the receiver also.
     */
    public void stop() {
        if (!started) return;
        synchronized (this) {
            try {
                started = false;
                if (processor != null) {
                    processor.stop();
                    processor = null;

                    for (int i = 0; i < rtpMgrs.length; i++) {
                        rtpMgrs[i].removeReceiveStreamListener(audioReceiver);
                        rtpMgrs[i].removeSessionListener(audioReceiver);
                        rtpMgrs[i].removeTargets("Session ended.");
                        rtpMgrs[i].dispose();
                    }

                    sendStreams.clear();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String createProcessor() {
        if (locator == null)
            return "Locator is null";

        DataSource ds;

        try {
            ds = javax.media.Manager.createDataSource(locator);
        } catch (Exception e) {
            e.printStackTrace();
            return "Couldn't create DataSource";
        }

        // Try to create a processor to handle the input jmf locator
        try {
            processor = javax.media.Manager.createProcessor(ds);
        } catch (NoProcessorException npe) {
            npe.printStackTrace();
            return "Couldn't create processor";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "IOException creating processor";
        }

        // Wait for it to configure
        boolean result = waitForState(processor, Processor.Configured);
        if (result == false)
            return "Couldn't configure processor";

        // Get the tracks from the processor
        TrackControl[] tracks = processor.getTrackControls();

        // Do we have atleast one track?
        if (tracks == null || tracks.length < 1)
            return "Couldn't find tracks in processor";

        // Set the output content descriptor to RAW_RTP
        // This will limit the supported formats reported from
        // Track.getSupportedFormats to only valid RTP formats.
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);

        Format supported[];
        Format chosen = null;
        boolean atLeastOneTrack = false;

        // Program the tracks.
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i].isEnabled()) {

                supported = tracks[i].getSupportedFormats();

                if (supported.length > 0) {
                    for (Format format : supported) {
                        if (format instanceof AudioFormat) {
                            if (this.format.matches(format))
                                chosen = format;
                        }
                    }
                    if (chosen != null) {
                        tracks[i].setFormat(chosen);
                        System.err.println("Track " + i + " is set to transmit as:");
                        System.err.println("  " + chosen);
                        atLeastOneTrack = true;
                    } else
                        tracks[i].setEnabled(false);
                } else
                    tracks[i].setEnabled(false);
            }
        }

        if (!atLeastOneTrack)
            return "Couldn't set any of the tracks to a valid RTP format";

        result = waitForState(processor, Controller.Realized);
        if (result == false)
            return "Couldn't realize processor";

        // Get the output data source of the processor
        dataOutput = processor.getDataOutput();

        return null;
    }


    /**
     * Use the RTPManager API to create sessions for each jmf
     * track of the processor.
     */
    private String createTransmitter() {

        // Cheated.  Should have checked the type.
        PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
        PushBufferStream pbss[] = pbds.getStreams();

        rtpMgrs = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        InetAddress ipAddr;
        SendStream sendStream;
        audioReceiver = new AudioReceiver(this);
        int port;
        SourceDescription srcDesList[];

        for (int i = 0; i < pbss.length; i++) {
            try {
                rtpMgrs[i] = RTPManager.newInstance();

                port = portBase + 2 * i;
                ipAddr = InetAddress.getByName(ipAddress);

                localAddr = new SessionAddress(InetAddress.getByName(this.localIpAddress),
                        localPort);

                destAddr = new SessionAddress(ipAddr, port);

                rtpMgrs[i].addReceiveStreamListener(audioReceiver);
                rtpMgrs[i].addSessionListener(audioReceiver);

                rtpMgrs[i].initialize(localAddr);

                rtpMgrs[i].addTarget(destAddr);

                System.err.println("Created RTP session at " + localPort + " to: " + ipAddress + " " + port);

                sendStream = rtpMgrs[i].createSendStream(dataOutput, i);

                sendStreams.add(sendStream);

                sendStream.start();

            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        return null;
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active
     */
    public void setTrasmit(boolean active) {
        for (SendStream sendStream : sendStreams) {
            try {
                if (active) {
                    sendStream.start();
                    System.out.println("START");
                } else {
                    sendStream.stop();
                    System.out.println("STOP");
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * *************************************************************
     * Convenience methods to handle processor's state changes.
     * **************************************************************
     */

    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }

    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        failed = false;

        // Call the required method on the processor
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }

        // Wait until we get an event that confirms the
        // success of the method, or a failure event.
        // See StateListener inner class
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }

        if (failed)
            return false;
        else
            return true;
    }

    /**
     * *************************************************************
     * Inner Classes
     * **************************************************************
     */

    class StateListener implements ControllerListener {

        public void controllerUpdate(ControllerEvent ce) {

            // If there was an error during configure or
            // realize, the processor will be closed
            if (ce instanceof ControllerClosedEvent)
                setFailed();

            // All controller events, send a notification
            // to the waiting thread in waitForState method.
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }

    public static void main(String args[]) {

        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();

            AudioChannel audioChannel0 = new AudioChannel(new MediaLocator("javasound://8000"), localhost.getHostAddress(), localhost.getHostAddress(), 7002, 7020, new AudioFormat(AudioFormat.GSM_RTP));
            AudioChannel audioChannel1 = new AudioChannel(new MediaLocator("javasound://8000"), localhost.getHostAddress(), localhost.getHostAddress(), 7020, 7002, new AudioFormat(AudioFormat.GSM_RTP));

            audioChannel0.start();
            audioChannel1.start();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            audioChannel0.setTrasmit(false);
            audioChannel1.setTrasmit(false);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            audioChannel0.setTrasmit(true);
            audioChannel1.setTrasmit(true);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            audioChannel0.stop();
            audioChannel1.stop();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }
}