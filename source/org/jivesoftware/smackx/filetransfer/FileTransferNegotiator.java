/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * Manages the negotiation of file transfers according to JEP-0096. If a file is
 * being sent the remote user chooses the type of stream under which the file
 * will be sent.
 *
 * @author Alexander Wenckus
 * @see <a href="http://xmpp.org/extensions/xep-0096.html">XEP-0096: SI File Transfer</a>
 */
public class FileTransferNegotiator {

    // Static

    private static final String[] NAMESPACE = {
            "http://jabber.org/protocol/si/profile/file-transfer",
            "http://jabber.org/protocol/si"};

    private static final Map<Connection, FileTransferNegotiator> transferObject =
            new ConcurrentHashMap<Connection, FileTransferNegotiator>();

    private static final String STREAM_INIT_PREFIX = "jsi_";

    protected static final String STREAM_DATA_FIELD_NAME = "stream-method";

    private static final Random randomGenerator = new Random();

    /**
     * A static variable to use only offer IBB for file transfer. It is generally recommend to only
     * set this variable to true for testing purposes as IBB is the backup file transfer method
     * and shouldn't be used as the only transfer method in production systems.
     */
    public static boolean IBB_ONLY = (System.getProperty("ibb") != null);//true;

    /**
     * Returns the file transfer negotiator related to a particular connection.
     * When this class is requested on a particular connection the file transfer
     * service is automatically enabled.
     *
     * @param connection The connection for which the transfer manager is desired
     * @return The IMFileTransferManager
     */
    public static FileTransferNegotiator getInstanceFor(
            final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (!connection.isConnected()) {
            return null;
        }

        if (transferObject.containsKey(connection)) {
            return transferObject.get(connection);
        }
        else {
            FileTransferNegotiator transfer = new FileTransferNegotiator(
                    connection);
            setServiceEnabled(connection, true);
            transferObject.put(connection, transfer);
            return transfer;
        }
    }

    /**
     * Enable the Jabber services related to file transfer on the particular
     * connection.
     *
     * @param connection The connection on which to enable or disable the services.
     * @param isEnabled  True to enable, false to disable.
     */
    public static void setServiceEnabled(final Connection connection,
            final boolean isEnabled) {
        ServiceDiscoveryManager manager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        List<String> namespaces = new ArrayList<String>();
        namespaces.addAll(Arrays.asList(NAMESPACE));
        namespaces.add(InBandBytestreamManager.NAMESPACE);
        if (!IBB_ONLY) {
            namespaces.add(Socks5BytestreamManager.NAMESPACE);
        }

        for (String namespace : namespaces) {
            if (isEnabled) {
                if (!manager.includesFeature(namespace)) {
                    manager.addFeature(namespace);
                }
            } else {
                manager.removeFeature(namespace);
            }
        }
        
    }

    /**
     * Checks to see if all file transfer related services are enabled on the
     * connection.
     *
     * @param connection The connection to check
     * @return True if all related services are enabled, false if they are not.
     */
    public static boolean isServiceEnabled(final Connection connection) {
        ServiceDiscoveryManager manager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        List<String> namespaces = new ArrayList<String>();
        namespaces.addAll(Arrays.asList(NAMESPACE));
        namespaces.add(InBandBytestreamManager.NAMESPACE);
        if (!IBB_ONLY) {
            namespaces.add(Socks5BytestreamManager.NAMESPACE);
        }

        for (String namespace : namespaces) {
            if (!manager.includesFeature(namespace)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A convenience method to create an IQ packet.
     *
     * @param ID   The packet ID of the
     * @param to   To whom the packet is addressed.
     * @param from From whom the packet is sent.
     * @param type The IQ type of the packet.
     * @return The created IQ packet.
     */
    public static IQ createIQ(final String ID, final String to,
            final String from, final IQ.Type type) {
        IQ iqPacket = new IQ() {
            public String getChildElementXML() {
                return null;
            }
        };
        iqPacket.setPacketID(ID);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);

        return iqPacket;
    }

    /**
     * Returns a collection of the supported transfer protocols.
     *
     * @return Returns a collection of the supported transfer protocols.
     */
    public static Collection<String> getSupportedProtocols() {
        List<String> protocols = new ArrayList<String>();
        protocols.add(InBandBytestreamManager.NAMESPACE);
        if (!IBB_ONLY) {
            protocols.add(Socks5BytestreamManager.NAMESPACE);
        }
        return Collections.unmodifiableList(protocols);
    }

    // non-static

    private final Connection connection;

    private final StreamNegotiator byteStreamTransferManager;

    private final StreamNegotiator inbandTransferManager;

    private FileTransferNegotiator(final Connection connection) {
        configureConnection(connection);

        this.connection = connection;
        byteStreamTransferManager = new Socks5TransferNegotiator(connection);
        inbandTransferManager = new IBBTransferNegotiator(connection);
    }

    private void configureConnection(final Connection connection) {
        connection.addConnectionListener(new ConnectionListener() {
            public void connectionClosed() {
                cleanup(connection);
            }

            public void connectionClosedOnError(Exception e) {
                cleanup(connection);
            }

            public void reconnectionFailed(Exception e) {
                // ignore
            }

            public void reconnectionSuccessful() {
                // ignore
            }

            public void reconnectingIn(int seconds) {
                // ignore
            }
        });
    }

    private void cleanup(final Connection connection) {
        if (transferObject.remove(connection) != null) {
            inbandTransferManager.cleanup();
        }
    }

    /**
     * Selects an appropriate stream negotiator after examining the incoming file transfer request.
     *
     * @param request The related file transfer request.
     * @return The file transfer object that handles the transfer
     * @throws XMPPException If there are either no stream methods contained in the packet, or
     *                       there is not an appropriate stream method.
     */
    public StreamNegotiator selectStreamNegotiator(
            FileTransferRequest request) throws XMPPException {
        StreamInitiation si = request.getStreamInitiation();
        FormField streamMethodField = getStreamMethodField(si
                .getFeatureNegotiationForm());

        if (streamMethodField == null) {
            String errorMessage = "No stream methods contained in packet.";
            XMPPError error = new XMPPError(XMPPError.Condition.bad_request, errorMessage);
            IQ iqPacket = createIQ(si.getPacketID(), si.getFrom(), si.getTo(),
                    IQ.Type.ERROR);
            iqPacket.setError(error);
            connection.sendPacket(iqPacket);
            throw new XMPPException(errorMessage, error);
        }

        // select the appropriate protocol

        StreamNegotiator selectedStreamNegotiator;
        try {
            selectedStreamNegotiator = getNegotiator(streamMethodField);
        }
        catch (XMPPException e) {
            IQ iqPacket = createIQ(si.getPacketID(), si.getFrom(), si.getTo(),
                    IQ.Type.ERROR);
            iqPacket.setError(e.getXMPPError());
            connection.sendPacket(iqPacket);
            throw e;
        }

        // return the appropriate negotiator

        return selectedStreamNegotiator;
    }

    private FormField getStreamMethodField(DataForm form) {
        FormField field = null;
        for (Iterator<FormField> it = form.getFields(); it.hasNext();) {
            field = it.next();
            if (field.getVariable().equals(STREAM_DATA_FIELD_NAME)) {
                break;
            }
            field = null;
        }
        return field;
    }

    private StreamNegotiator getNegotiator(final FormField field)
            throws XMPPException {
        String variable;
        boolean isByteStream = false;
        boolean isIBB = false;
        for (Iterator<FormField.Option> it = field.getOptions(); it.hasNext();) {
            variable = it.next().getValue();
            if (variable.equals(Socks5BytestreamManager.NAMESPACE) && !IBB_ONLY) {
                isByteStream = true;
            }
            else if (variable.equals(InBandBytestreamManager.NAMESPACE)) {
                isIBB = true;
            }
        }

        if (!isByteStream && !isIBB) {
            XMPPError error = new XMPPError(XMPPError.Condition.bad_request,
                    "No acceptable transfer mechanism");
            throw new XMPPException(error.getMessage(), error);
        }

       //if (isByteStream && isIBB && field.getType().equals(FormField.TYPE_LIST_MULTI)) {
        if (isByteStream && isIBB) { 
            return new FaultTolerantNegotiator(connection,
                    byteStreamTransferManager,
                    inbandTransferManager);
        }
        else if (isByteStream) {
            return byteStreamTransferManager;
        }
        else {
            return inbandTransferManager;
        }
    }

    /**
     * Reject a stream initiation request from a remote user.
     *
     * @param si The Stream Initiation request to reject.
     */
    public void rejectStream(final StreamInitiation si) {
        XMPPError error = new XMPPError(XMPPError.Condition.forbidden, "Offer Declined");
        IQ iqPacket = createIQ(si.getPacketID(), si.getFrom(), si.getTo(),
                IQ.Type.ERROR);
        iqPacket.setError(error);
        connection.sendPacket(iqPacket);
    }

    /**
     * Returns a new, unique, stream ID to identify a file transfer.
     *
     * @return Returns a new, unique, stream ID to identify a file transfer.
     */
    public String getNextStreamID() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(STREAM_INIT_PREFIX);
        buffer.append(Math.abs(randomGenerator.nextLong()));

        return buffer.toString();
    }

    /**
     * Send a request to another user to send them a file. The other user has
     * the option of, accepting, rejecting, or not responding to a received file
     * transfer request.
     * <p/>
     * If they accept, the packet will contain the other user's chosen stream
     * type to send the file across. The two choices this implementation
     * provides to the other user for file transfer are <a
     * href="http://www.jabber.org/jeps/jep-0065.html">SOCKS5 Bytestreams</a>,
     * which is the preferred method of transfer, and <a
     * href="http://www.jabber.org/jeps/jep-0047.html">In-Band Bytestreams</a>,
     * which is the fallback mechanism.
     * <p/>
     * The other user may choose to decline the file request if they do not
     * desire the file, their client does not support JEP-0096, or if there are
     * no acceptable means to transfer the file.
     * <p/>
     * Finally, if the other user does not respond this method will return null
     * after the specified timeout.
     *
     * @param userID          The userID of the user to whom the file will be sent.
     * @param streamID        The unique identifier for this file transfer.
     * @param fileName        The name of this file. Preferably it should include an
     *                        extension as it is used to determine what type of file it is.
     * @param size            The size, in bytes, of the file.
     * @param desc            A description of the file.
     * @param responseTimeout The amount of time, in milliseconds, to wait for the remote
     *                        user to respond. If they do not respond in time, this
     * @return Returns the stream negotiator selected by the peer.
     * @throws XMPPException Thrown if there is an error negotiating the file transfer.
     */
    public StreamNegotiator negotiateOutgoingTransfer(final String userID,
            final String streamID, final String fileName, final long size,
            final String desc, int responseTimeout) throws XMPPException {
        StreamInitiation si = new StreamInitiation();
        si.setSesssionID(streamID);
        si.setMimeType(URLConnection.guessContentTypeFromName(fileName));

        StreamInitiation.File siFile = new StreamInitiation.File(fileName, size);
        siFile.setDesc(desc);
        si.setFile(siFile);

        si.setFeatureNegotiationForm(createDefaultInitiationForm());

        si.setFrom(connection.getUser());
        si.setTo(userID);
        si.setType(IQ.Type.SET);

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(si.getPacketID()));
        connection.sendPacket(si);
        Packet siResponse = collector.nextResult(responseTimeout);
        collector.cancel();

        if (siResponse instanceof IQ) {
            IQ iqResponse = (IQ) siResponse;
            if (iqResponse.getType().equals(IQ.Type.RESULT)) {
                StreamInitiation response = (StreamInitiation) siResponse;
                return getOutgoingNegotiator(getStreamMethodField(response
                        .getFeatureNegotiationForm()));

            }
            else if (iqResponse.getType().equals(IQ.Type.ERROR)) {
                throw new XMPPException(iqResponse.getError());
            }
            else {
                throw new XMPPException("File transfer response unreadable");
            }
        }
        else {
            return null;
        }
    }

    private StreamNegotiator getOutgoingNegotiator(final FormField field)
            throws XMPPException {
        String variable;
        boolean isByteStream = false;
        boolean isIBB = false;
        for (Iterator<String> it = field.getValues(); it.hasNext();) {
            variable = it.next();
            if (variable.equals(Socks5BytestreamManager.NAMESPACE) && !IBB_ONLY) {
                isByteStream = true;
            }
            else if (variable.equals(InBandBytestreamManager.NAMESPACE)) {
                isIBB = true;
            }
        }

        if (!isByteStream && !isIBB) {
            XMPPError error = new XMPPError(XMPPError.Condition.bad_request,
                    "No acceptable transfer mechanism");
            throw new XMPPException(error.getMessage(), error);
        }

        if (isByteStream && isIBB) {
            return new FaultTolerantNegotiator(connection,
                    byteStreamTransferManager, inbandTransferManager);
        }
        else if (isByteStream) {
            return byteStreamTransferManager;
        }
        else {
            return inbandTransferManager;
        }
    }

    private DataForm createDefaultInitiationForm() {
        DataForm form = new DataForm(Form.TYPE_FORM);
        FormField field = new FormField(STREAM_DATA_FIELD_NAME);
        field.setType(FormField.TYPE_LIST_SINGLE);
        if (!IBB_ONLY) {
            field.addOption(new FormField.Option(Socks5BytestreamManager.NAMESPACE));
        }
        field.addOption(new FormField.Option(InBandBytestreamManager.NAMESPACE));
        form.addField(field);
        return form;
    }
}
