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

package org.jivesoftware.smackx.jingle.mediaimpl.jmf;

import javax.media.*;
import javax.media.protocol.DataSource;
import javax.media.rtp.*;
import javax.media.rtp.event.*;

/**
 * This class implements receive methods and listeners to be used in AudioChannel
 *
 * @author Thiago Camargo 
 */
public class AudioReceiver implements ReceiveStreamListener, SessionListener,
        ControllerListener {

    boolean dataReceived = false;

    Object dataSync;

    public AudioReceiver(final Object dataSync) {
        this.dataSync = dataSync;
    }

    /**
     * JingleSessionListener.
     */
    public synchronized void update(SessionEvent evt) {
        if (evt instanceof NewParticipantEvent) {
            Participant p = ((NewParticipantEvent) evt).getParticipant();
            System.err.println("  - A new participant had just joined: " + p.getCNAME());
        }
    }

    /**
     * ReceiveStreamListener
     */
    public synchronized void update(ReceiveStreamEvent evt) {

        Participant participant = evt.getParticipant();    // could be null.
        ReceiveStream stream = evt.getReceiveStream();  // could be null.

        if (evt instanceof RemotePayloadChangeEvent) {
            System.err.println("  - Received an RTP PayloadChangeEvent.");
            System.err.println("Sorry, cannot handle payload change.");

        } else if (evt instanceof NewReceiveStreamEvent) {

            try {
                stream = evt.getReceiveStream();
                DataSource ds = stream.getDataSource();

                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                if (ctl != null) {
                    System.err.println("  - Recevied new RTP stream: " + ctl.getFormat());
                } else
                    System.err.println("  - Recevied new RTP stream");

                if (participant == null)
                    System.err.println("      The sender of this stream had yet to be identified.");
                else {
                    System.err.println("      The stream comes from: " + participant.getCNAME());
                }

                // create a player by passing datasource to the Media Manager
                Player p = javax.media.Manager.createPlayer(ds);
                if (p == null)
                    return;

                p.addControllerListener(this);
                p.realize();

                // Notify intialize() that a new stream had arrived.
                synchronized (dataSync) {
                    dataReceived = true;
                    dataSync.notifyAll();
                }

            } catch (Exception e) {
                System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
                return;
            }

        } else if (evt instanceof StreamMappedEvent) {

            if (stream != null && stream.getDataSource() != null) {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                System.err.println("  - The previously unidentified stream ");
                if (ctl != null)
                    System.err.println("      " + ctl.getFormat());
                System.err.println("      had now been identified as sent by: " + participant.getCNAME());
            }
        } else if (evt instanceof ByeEvent) {

            System.err.println("  - Got \"bye\" from: " + participant.getCNAME());

        }

    }

    /**
     * ControllerListener for the Players.
     */
    public synchronized void controllerUpdate(ControllerEvent ce) {

        Player p = (Player) ce.getSourceController();

        if (p == null)
            return;

        // Get this when the internal players are realized.
        if (ce instanceof RealizeCompleteEvent) {
            p.start();
        }

        if (ce instanceof ControllerErrorEvent) {
            p.removeControllerListener(this);
            System.err.println("Receiver internal error: " + ce);
        }

    }
}
