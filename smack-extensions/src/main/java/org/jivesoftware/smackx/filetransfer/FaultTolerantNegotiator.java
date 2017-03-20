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
package org.jivesoftware.smackx.filetransfer;

import java.io.InputStream;
import java.io.OutputStream;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.jid.Jid;


/**
 * The fault tolerant negotiator takes two stream negotiators, the primary and the secondary
 * negotiator. If the primary negotiator fails during the stream negotiaton process, the second
 * negotiator is used.
 */
public class FaultTolerantNegotiator extends StreamNegotiator {

    private final StreamNegotiator primaryNegotiator;
    private final StreamNegotiator secondaryNegotiator;

    public FaultTolerantNegotiator(XMPPConnection connection, StreamNegotiator primary,
            StreamNegotiator secondary) {
        super(connection);
        this.primaryNegotiator = primary;
        this.secondaryNegotiator = secondary;
    }

    @Override
    public void newStreamInitiation(Jid from, String streamID) {
        primaryNegotiator.newStreamInitiation(from, streamID);
        secondaryNegotiator.newStreamInitiation(from, streamID);
    }

    @Override
    InputStream negotiateIncomingStream(Stanza streamInitiation) {
        throw new UnsupportedOperationException("Negotiation only handled by create incoming " +
                "stream method.");
    }

    @Override
    public InputStream createIncomingStream(final StreamInitiation initiation) throws SmackException, XMPPErrorException, InterruptedException {
        // This could be either an xep47 ibb 'open' iq or an xep65 streamhost iq
        IQ initationSet = initiateIncomingStream(connection(), initiation);

        StreamNegotiator streamNegotiator = determineNegotiator(initationSet);

        return streamNegotiator.negotiateIncomingStream(initationSet);
    }

    private StreamNegotiator determineNegotiator(Stanza streamInitiation) {
        if (streamInitiation instanceof Bytestream) {
            return primaryNegotiator;
        } else if (streamInitiation instanceof Open){
            return secondaryNegotiator;
        } else {
            throw new IllegalStateException("Unknown stream initation type");
        }
    }

    @Override
    public OutputStream createOutgoingStream(String streamID, Jid initiator, Jid target)
                    throws SmackException, XMPPException, InterruptedException {
        OutputStream stream;
        try {
            stream = primaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }
        catch (Exception ex) {
            stream = secondaryNegotiator.createOutgoingStream(streamID, initiator, target);
        }

        return stream;
    }

    @Override
    public String[] getNamespaces() {
        String[] primary = primaryNegotiator.getNamespaces();
        String[] secondary = secondaryNegotiator.getNamespaces();

        String[] namespaces = new String[primary.length + secondary.length];
        System.arraycopy(primary, 0, namespaces, 0, primary.length);
        System.arraycopy(secondary, 0, namespaces, primary.length, secondary.length);

        return namespaces;
    }

}
