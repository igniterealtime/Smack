/**
 * $RCSfile: AudioReceiver.java,v $
 * $Revision: 1.1 $
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

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.protocol.DataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;

import org.jivesoftware.smackx.jingle.SmackLogger;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;

/**
 * This class implements receive methods and listeners to be used in AudioChannel
 *
 * @author Thiago Camargo
 */
public class AudioReceiver implements ReceiveStreamListener, SessionListener,
        ControllerListener {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(AudioReceiver.class);

	boolean dataReceived = false;

    Object dataSync;
    JingleMediaSession jingleMediaSession;

    public AudioReceiver(final Object dataSync, final JingleMediaSession jingleMediaSession) {
        this.dataSync = dataSync;
        this.jingleMediaSession = jingleMediaSession;
    }

    /**
     * JingleSessionListener.
     */
    public synchronized void update(SessionEvent evt) {
        if (evt instanceof NewParticipantEvent) {
            Participant p = ((NewParticipantEvent) evt).getParticipant();
            LOGGER.error("  - A new participant had just joined: " + p.getCNAME());
        }
    }

    /**
     * ReceiveStreamListener
     */
    public synchronized void update(ReceiveStreamEvent evt) {

        Participant participant = evt.getParticipant();    // could be null.
        ReceiveStream stream = evt.getReceiveStream();  // could be null.

        if (evt instanceof RemotePayloadChangeEvent) {
            LOGGER.error("  - Received an RTP PayloadChangeEvent.");
            LOGGER.error("Sorry, cannot handle payload change.");

        }
        else if (evt instanceof NewReceiveStreamEvent) {

            try {
                stream = evt.getReceiveStream();
                DataSource ds = stream.getDataSource();

                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                if (ctl != null) {
                    LOGGER.error("  - Recevied new RTP stream: " + ctl.getFormat());
                }
                else
                    LOGGER.error("  - Recevied new RTP stream");

                if (participant == null)
                    LOGGER.error("      The sender of this stream had yet to be identified.");
                else {
                    LOGGER.error("      The stream comes from: " + participant.getCNAME());
                }

                // create a player by passing datasource to the Media Manager
                Player p = javax.media.Manager.createPlayer(ds);
                if (p == null)
                    return;

                p.addControllerListener(this);
                p.realize();
                jingleMediaSession.mediaReceived(participant != null ? participant.getCNAME() : "");

                // Notify intialize() that a new stream had arrived.
                synchronized (dataSync) {
                    dataReceived = true;
                    dataSync.notifyAll();
                }

            }
            catch (Exception e) {
                LOGGER.error("NewReceiveStreamEvent exception " + e.getMessage());
                return;
            }

        }
        else if (evt instanceof StreamMappedEvent) {

            if (stream != null && stream.getDataSource() != null) {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.jmf.rtp.RTPControl");
                LOGGER.error("  - The previously unidentified stream ");
                if (ctl != null)
                    LOGGER.error("      " + ctl.getFormat());
                LOGGER.error("      had now been identified as sent by: " + participant.getCNAME());
            }
        }
        else if (evt instanceof ByeEvent) {

            LOGGER.error("  - Got \"bye\" from: " + participant.getCNAME());

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
            LOGGER.error("Receiver internal error: " + ce);
        }

    }
}
