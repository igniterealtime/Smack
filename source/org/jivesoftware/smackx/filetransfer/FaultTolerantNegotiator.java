/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.filetransfer;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.packet.StreamInitiation;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The fault tolerant negotiator takes two stream negotiators, the primary and the secondary negotiator.
 * If the primary negotiator fails during the stream negotiaton process, the second negotiator is used.
 */
public class FaultTolerantNegotiator extends StreamNegotiator {

    private StreamNegotiator primaryNegotiator;
    private StreamNegotiator secondaryNegotiator;
    private XMPPConnection connection;
    private PacketFilter primaryFilter;
    private PacketFilter secondaryFilter;

    public FaultTolerantNegotiator(XMPPConnection connection, StreamNegotiator primary, StreamNegotiator secondary) {
        this.primaryNegotiator = primary;
        this.secondaryNegotiator = secondary;
        this.connection = connection;
    }

    public PacketFilter getInitiationPacketFilter(String from, String streamID) {
        if (primaryFilter == null || secondaryFilter == null) {
            primaryFilter = primaryNegotiator.getInitiationPacketFilter(from, streamID);
            secondaryFilter = secondaryNegotiator.getInitiationPacketFilter(from, streamID);
        }
        return new OrFilter(primaryFilter, secondaryFilter);
    }

    InputStream negotiateIncomingStream(Packet streamInitiation) throws XMPPException {
        throw new UnsupportedOperationException("Negotiation only handled by create incoming stream method.");
    }

    final Packet initiateIncomingStream(XMPPConnection connection, StreamInitiation initiation) throws XMPPException {
        throw new UnsupportedOperationException("Initiation handled by createIncomingStream method");
    }

    public InputStream createIncomingStream(StreamInitiation initiation) throws XMPPException {
        PacketFilter filter = getInitiationPacketFilter(initiation.getFrom(), initiation.getSessionID());
        PacketCollector collector = connection.createPacketCollector(filter);

        StreamInitiation response = super.createInitiationAccept(initiation, getNamespaces());
        connection.sendPacket(response);

        InputStream stream;
        try {
            Packet streamInitiation = collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
            if (streamInitiation == null) {
                throw new XMPPException("No response from remote client");
            }
            StreamNegotiator negotiator = determineNegotiator(streamInitiation);
            stream = negotiator.negotiateIncomingStream(streamInitiation);
        }
        catch (XMPPException ex) {
            ex.printStackTrace();
            Packet streamInitiation = collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
            collector.cancel();
            if (streamInitiation == null) {
                throw new XMPPException("No response from remote client");
            }
            StreamNegotiator negotiator = determineNegotiator(streamInitiation);
            stream = negotiator.negotiateIncomingStream(streamInitiation);
        } finally {
            collector.cancel();
        }

        return stream;
    }

    private StreamNegotiator determineNegotiator(Packet streamInitiation) {
        return primaryFilter.accept(streamInitiation) ? primaryNegotiator : secondaryNegotiator;
    }

    public OutputStream createOutgoingStream(String streamID, String initiator, String target) throws XMPPException {
        OutputStream stream;
        try {
            stream = primaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }
        catch (XMPPException ex) {
            stream = secondaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }

        return stream;
    }

    public String[] getNamespaces() {
        String [] primary = primaryNegotiator.getNamespaces();
        String [] secondary = secondaryNegotiator.getNamespaces();

        String [] namespaces = new String[primary.length + secondary.length];
        System.arraycopy(primary, 0, namespaces, 0, primary.length);
        System.arraycopy(secondary, 0, namespaces, primary.length, secondary.length);

        return namespaces;
    }

    public void cleanup() {
    }

}
