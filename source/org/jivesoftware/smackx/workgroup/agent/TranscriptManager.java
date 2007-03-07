/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smackx.workgroup.packet.Transcript;
import org.jivesoftware.smackx.workgroup.packet.Transcripts;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;

/**
 * A TranscriptManager helps to retrieve the full conversation transcript of a given session
 * {@link #getTranscript(String, String)} or to retrieve a list with the summary of all the
 * conversations that a user had {@link #getTranscripts(String, String)}.
 *
 * @author Gaston Dombiak
 */
public class TranscriptManager {
    private XMPPConnection connection;

    public TranscriptManager(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns the full conversation transcript of a given session.
     *
     * @param sessionID the id of the session to get the full transcript.
     * @param workgroupJID the JID of the workgroup that will process the request.
     * @return the full conversation transcript of a given session.
     * @throws XMPPException if an error occurs while getting the information.
     */
    public Transcript getTranscript(String workgroupJID, String sessionID) throws XMPPException {
        Transcript request = new Transcript(sessionID);
        request.setTo(workgroupJID);
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        // Send the request
        connection.sendPacket(request);

        Transcript response = (Transcript) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }

    /**
     * Returns the transcripts of a given user. The answer will contain the complete history of
     * conversations that a user had.
     *
     * @param userID the id of the user to get his conversations.
     * @param workgroupJID the JID of the workgroup that will process the request.
     * @return the transcripts of a given user.
     * @throws XMPPException if an error occurs while getting the information.
     */
    public Transcripts getTranscripts(String workgroupJID, String userID) throws XMPPException {
        Transcripts request = new Transcripts(userID);
        request.setTo(workgroupJID);
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
        // Send the request
        connection.sendPacket(request);

        Transcripts response = (Transcripts) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return response;
    }
}
