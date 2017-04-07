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

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.EventManger;
import org.jivesoftware.smack.util.EventManger.Callback;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * After the file transfer negotiation process is completed according to
 * XEP-0096, the negotiation process is passed off to a particular stream
 * negotiator. The stream negotiator will then negotiate the chosen stream and
 * return the stream to transfer the file.
 *
 * @author Alexander Wenckus
 */
public abstract class StreamNegotiator extends Manager {

    protected StreamNegotiator(XMPPConnection connection) {
        super(connection);
    }

    /**
     * A event manager for stream initiation requests send to us.
     * <p>
     * Those are typical XEP-45 Open or XEP-65 Bytestream IQ requests. The even key is in the format
     * "initiationFrom + '\t' + streamId"
     * </p>
     */
    // TODO This field currently being static is considered a quick hack. Ideally this should take
    // the local connection into account, for example by changing the key to
    // "localJid + '\t' + initiationFrom + '\t' + streamId" or making the field non-static (but then
    // you need to provide access to the InitiationListeners, which could get tricky)
    protected static final EventManger<String, IQ, SmackException.NotConnectedException> initationSetEvents = new EventManger<>();

    /**
     * Creates the initiation acceptance stanza(/packet) to forward to the stream
     * initiator.
     *
     * @param streamInitiationOffer The offer from the stream initiator to connect for a stream.
     * @param namespaces            The namespace that relates to the accepted means of transfer.
     * @return The response to be forwarded to the initiator.
     */
    protected static StreamInitiation createInitiationAccept(
            StreamInitiation streamInitiationOffer, String[] namespaces)
    {
        StreamInitiation response = new StreamInitiation();
        response.setTo(streamInitiationOffer.getFrom());
        response.setFrom(streamInitiationOffer.getTo());
        response.setType(IQ.Type.result);
        response.setStanzaId(streamInitiationOffer.getStanzaId());

        DataForm form = new DataForm(DataForm.Type.submit);
        FormField field = new FormField(
                FileTransferNegotiator.STREAM_DATA_FIELD_NAME);
        for (String namespace : namespaces) {
            field.addValue(namespace);
        }
        form.addField(field);

        response.setFeatureNegotiationForm(form);
        return response;
    }

    protected final IQ initiateIncomingStream(final XMPPConnection connection, StreamInitiation initiation)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        final StreamInitiation response = createInitiationAccept(initiation,
                getNamespaces());

        newStreamInitiation(initiation.getFrom(), initiation.getSessionID());

        final String eventKey = initiation.getFrom().toString() + '\t' + initiation.getSessionID();
        IQ streamMethodInitiation;
        try {
            streamMethodInitiation = initationSetEvents.performActionAndWaitForEvent(eventKey, connection.getReplyTimeout(), new Callback<NotConnectedException>() {
                @Override
                public void action() throws NotConnectedException {
                    try {
                        connection.sendStanza(response);
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
            });
        }
        catch (InterruptedException e) {
            // TODO remove this try/catch once merged into 4.2's master branch
            throw new IllegalStateException(e);
        }

        if (streamMethodInitiation == null) {
            throw NoResponseException.newWith(connection, "stream initiation");
        }
        XMPPErrorException.ifHasErrorThenThrow(streamMethodInitiation);
        return streamMethodInitiation;
    }

    /**
     * Signal that a new stream initiation arrived. The negotiator may needs to prepare for it.
     *
     * @param from     The initiator of the file transfer.
     * @param streamID The stream ID related to the transfer.
     */
    protected abstract void newStreamInitiation(Jid from, String streamID);


    abstract InputStream negotiateIncomingStream(Stanza streamInitiation) throws XMPPErrorException,
            InterruptedException, NoResponseException, SmackException;

    /**
     * This method handles the file stream download negotiation process. The
     * appropriate stream negotiator's initiate incoming stream is called after
     * an appropriate file transfer method is selected. The manager will respond
     * to the initiator with the selected means of transfer, then it will handle
     * any negotiation specific to the particular transfer method. This method
     * returns the InputStream, ready to transfer the file.
     *
     * @param initiation The initiation that triggered this download.
     * @return After the negotiation process is complete, the InputStream to
     *         write a file to is returned.
     * @throws XMPPErrorException If an error occurs during this process an XMPPException is
     *                       thrown.
     * @throws InterruptedException If thread is interrupted.
     * @throws SmackException 
     */
    public abstract InputStream createIncomingStream(StreamInitiation initiation)
            throws XMPPErrorException, InterruptedException, NoResponseException, SmackException;

    /**
     * This method handles the file upload stream negotiation process. The
     * particular stream negotiator is determined during the file transfer
     * negotiation process. This method returns the OutputStream to transmit the
     * file to the remote user.
     *
     * @param streamID  The streamID that uniquely identifies the file transfer.
     * @param initiator The fully-qualified JID of the initiator of the file transfer.
     * @param target    The fully-qualified JID of the target or receiver of the file
     *                  transfer.
     * @return The negotiated stream ready for data.
     * @throws XMPPErrorException If an error occurs during the negotiation process an
     *                       exception will be thrown.
     * @throws SmackException 
     * @throws XMPPException 
     * @throws InterruptedException 
     */
    public abstract OutputStream createOutgoingStream(String streamID,
            Jid initiator, Jid target) throws XMPPErrorException, NoResponseException, SmackException, XMPPException, InterruptedException;

    /**
     * Returns the XMPP namespace reserved for this particular type of file
     * transfer.
     *
     * @return Returns the XMPP namespace reserved for this particular type of
     *         file transfer.
     */
    public abstract String[] getNamespaces();

    public static void signal(String eventKey, IQ eventValue) {
        initationSetEvents.signalEvent(eventKey, eventValue);
    }
}
