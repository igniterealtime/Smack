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

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;

import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransferException.NoAcceptableTransferMechanisms;
import org.jivesoftware.smackx.filetransfer.FileTransferException.NoStreamMethodsOfferedException;
import org.jivesoftware.smackx.formtypes.FormFieldRegistry;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.ListSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
 * Manages the negotiation of file transfers according to XEP-0096. If a file is
 * being sent the remote user chooses the type of stream under which the file
 * will be sent.
 *
 * @author Alexander Wenckus
 * @see <a href="http://xmpp.org/extensions/xep-0096.html">XEP-0096: SI File Transfer</a>
 */
public final class FileTransferNegotiator extends Manager {

    public static final String SI_NAMESPACE = "http://jabber.org/protocol/si";
    public static final String SI_PROFILE_FILE_TRANSFER_NAMESPACE = "http://jabber.org/protocol/si/profile/file-transfer";
    private static final String[] NAMESPACE = { SI_NAMESPACE, SI_PROFILE_FILE_TRANSFER_NAMESPACE };

    private static final Map<XMPPConnection, FileTransferNegotiator> INSTANCES = new WeakHashMap<>();

    private static final String STREAM_INIT_PREFIX = "jsi_";

    static final String STREAM_DATA_FIELD_NAME = "stream-method";
    static {
        FormFieldRegistry.addLookasideFieldRegistryEntry(STREAM_DATA_FIELD_NAME, FormField.Type.list_single);
    }

    private static final Random randomGenerator = new Random();

    /**
     * A static variable to use only offer IBB for file transfer. It is generally recommend to only
     * set this variable to true for testing purposes as IBB is the backup file transfer method
     * and shouldn't be used as the only transfer method in production systems.
     */
    public static boolean IBB_ONLY = System.getProperty("ibb") != null;//true;

    /**
     * Returns the file transfer negotiator related to a particular connection.
     * When this class is requested on a particular connection the file transfer
     * service is automatically enabled.
     *
     * @param connection The connection for which the transfer manager is desired
     * @return The FileTransferNegotiator
     */
    public static synchronized FileTransferNegotiator getInstanceFor(
            final XMPPConnection connection) {
        FileTransferNegotiator fileTransferNegotiator = INSTANCES.get(connection);
        if (fileTransferNegotiator == null) {
            fileTransferNegotiator = new FileTransferNegotiator(connection);
            INSTANCES.put(connection, fileTransferNegotiator);
        }
        return fileTransferNegotiator;
    }

    /**
     * Enable the Jabber services related to file transfer on the particular
     * connection.
     *
     * @param connection The connection on which to enable or disable the services.
     * @param isEnabled  True to enable, false to disable.
     */
    private static void setServiceEnabled(final XMPPConnection connection,
            final boolean isEnabled) {
        ServiceDiscoveryManager manager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        List<String> namespaces = new ArrayList<>();
        namespaces.addAll(Arrays.asList(NAMESPACE));
        namespaces.add(DataPacketExtension.NAMESPACE);
        if (!IBB_ONLY) {
            namespaces.add(Bytestream.NAMESPACE);
        }

        for (String namespace : namespaces) {
            if (isEnabled) {
                manager.addFeature(namespace);
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
    public static boolean isServiceEnabled(final XMPPConnection connection) {
        ServiceDiscoveryManager manager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        List<String> namespaces = new ArrayList<>();
        namespaces.addAll(Arrays.asList(NAMESPACE));
        namespaces.add(DataPacketExtension.NAMESPACE);
        if (!IBB_ONLY) {
            namespaces.add(Bytestream.NAMESPACE);
        }

        for (String namespace : namespaces) {
            if (!manager.includesFeature(namespace)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a collection of the supported transfer protocols.
     *
     * @return Returns a collection of the supported transfer protocols.
     */
    public static Collection<String> getSupportedProtocols() {
        List<String> protocols = new ArrayList<>();
        protocols.add(DataPacketExtension.NAMESPACE);
        if (!IBB_ONLY) {
            protocols.add(Bytestream.NAMESPACE);
        }
        return Collections.unmodifiableList(protocols);
    }

    // non-static

    private final StreamNegotiator byteStreamTransferManager;

    private final StreamNegotiator inbandTransferManager;

    private FileTransferNegotiator(final XMPPConnection connection) {
        super(connection);
        byteStreamTransferManager = new Socks5TransferNegotiator(connection);
        inbandTransferManager = new IBBTransferNegotiator(connection);

        setServiceEnabled(connection, true);
    }

    /**
     * Selects an appropriate stream negotiator after examining the incoming file transfer request.
     *
     * @param request The related file transfer request.
     * @return The file transfer object that handles the transfer
     * @throws NoStreamMethodsOfferedException If there are either no stream methods contained in the packet, or
     *                       there is not an appropriate stream method.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NoAcceptableTransferMechanisms if no acceptable transfer mechanisms are available
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public StreamNegotiator selectStreamNegotiator(
            FileTransferRequest request) throws NotConnectedException, NoStreamMethodsOfferedException, NoAcceptableTransferMechanisms, InterruptedException {
        StreamInitiation si = request.getStreamInitiation();
        ListSingleFormField streamMethodField = getStreamMethodField(si
                .getFeatureNegotiationForm());

        if (streamMethodField == null) {
            String errorMessage = "No stream methods contained in stanza.";
            StanzaError error = StanzaError.from(StanzaError.Condition.bad_request, errorMessage).build();
            IQ iqPacket = IQ.createErrorResponse(si, error);
            connection().sendStanza(iqPacket);
            throw new FileTransferException.NoStreamMethodsOfferedException();
        }

        // select the appropriate protocol
        StreamNegotiator selectedStreamNegotiator;
        try {
            selectedStreamNegotiator = getNegotiator(streamMethodField);
        }
        catch (NoAcceptableTransferMechanisms e) {
            IQ iqPacket = IQ.createErrorResponse(si, StanzaError.from(StanzaError.Condition.bad_request, "No acceptable transfer mechanism").build());
            connection().sendStanza(iqPacket);
            throw e;
        }

        // return the appropriate negotiator

        return selectedStreamNegotiator;
    }

    private static ListSingleFormField getStreamMethodField(DataForm form) {
        return (ListSingleFormField) form.getField(STREAM_DATA_FIELD_NAME);
    }

    private StreamNegotiator getNegotiator(final ListSingleFormField field)
            throws NoAcceptableTransferMechanisms {
        String variable;
        boolean isByteStream = false;
        boolean isIBB = false;
        for (FormField.Option option : field.getOptions()) {
            variable = option.getValueString();
            if (variable.equals(Bytestream.NAMESPACE) && !IBB_ONLY) {
                isByteStream = true;
            }
            else if (variable.equals(DataPacketExtension.NAMESPACE)) {
                isIBB = true;
            }
        }

        if (!isByteStream && !isIBB) {
            throw new FileTransferException.NoAcceptableTransferMechanisms();
        }

        if (isByteStream) {
            return byteStreamTransferManager;
        }
        else {
            return inbandTransferManager;
        }
    }

    /**
     * Returns a new, unique, stream ID to identify a file transfer.
     *
     * @return Returns a new, unique, stream ID to identify a file transfer.
     */
    public static String getNextStreamID() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(STREAM_INIT_PREFIX);
        buffer.append(randomGenerator.nextInt(Integer.MAX_VALUE) + randomGenerator.nextInt(Integer.MAX_VALUE));

        return buffer.toString();
    }

    /**
     * Send a request to another user to send them a file. The other user has
     * the option of, accepting, rejecting, or not responding to a received file
     * transfer request.
     * <p>
     * If they accept, the stanza will contain the other user's chosen stream
     * type to send the file across. The two choices this implementation
     * provides to the other user for file transfer are <a
     * href="http://www.xmpp.org/extensions/jep-0065.html">SOCKS5 Bytestreams</a>,
     * which is the preferred method of transfer, and <a
     * href="http://www.xmpp.org/extensions/jep-0047.html">In-Band Bytestreams</a>,
     * which is the fallback mechanism.
     * </p>
     * <p>
     * The other user may choose to decline the file request if they do not
     * desire the file, their client does not support XEP-0096, or if there are
     * no acceptable means to transfer the file.
     * </p>
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
     * @throws XMPPErrorException Thrown if there is an error negotiating the file transfer.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NoAcceptableTransferMechanisms if no acceptable transfer mechanisms are available
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public StreamNegotiator negotiateOutgoingTransfer(final Jid userID,
            final String streamID, final String fileName, final long size,
            final String desc, int responseTimeout) throws XMPPErrorException, NotConnectedException, NoResponseException, NoAcceptableTransferMechanisms, InterruptedException {
        StreamInitiation si = new StreamInitiation();
        si.setSessionID(streamID);
        si.setMimeType(URLConnection.guessContentTypeFromName(fileName));

        StreamInitiation.File siFile = new StreamInitiation.File(fileName, size);
        siFile.setDesc(desc);
        si.setFile(siFile);

        si.setFeatureNegotiationForm(createDefaultInitiationForm());

        si.setFrom(connection().getUser());
        si.setTo(userID);
        si.setType(IQ.Type.set);

        Stanza siResponse = connection().createStanzaCollectorAndSend(si).nextResultOrThrow(
                        responseTimeout);

        if (siResponse instanceof IQ) {
            IQ iqResponse = (IQ) siResponse;
            if (iqResponse.getType().equals(IQ.Type.result)) {
                StreamInitiation response = (StreamInitiation) siResponse;
                return getOutgoingNegotiator(getStreamMethodField(response
                        .getFeatureNegotiationForm()));

            }
            else {
                throw new XMPPErrorException(iqResponse, iqResponse.getError());
            }
        }
        else {
            return null;
        }
    }

    private StreamNegotiator getOutgoingNegotiator(final FormField field) throws NoAcceptableTransferMechanisms {
        boolean isByteStream = false;
        boolean isIBB = false;
        for (CharSequence variable : field.getValues()) {
            String variableString = variable.toString();
            if (variableString.equals(Bytestream.NAMESPACE) && !IBB_ONLY) {
                isByteStream = true;
            }
            else if (variableString.equals(DataPacketExtension.NAMESPACE)) {
                isIBB = true;
            }
        }

        if (!isByteStream && !isIBB) {
            throw new FileTransferException.NoAcceptableTransferMechanisms();
        }

        if (isByteStream) {
            return byteStreamTransferManager;
        }
        else {
            return inbandTransferManager;
        }
    }

    private static DataForm createDefaultInitiationForm() {
        DataForm.Builder form = DataForm.builder(DataForm.Type.form);
        ListSingleFormField.Builder fieldBuilder = FormField.listSingleBuilder(STREAM_DATA_FIELD_NAME);

        if (!IBB_ONLY) {
            fieldBuilder.addOption(Bytestream.NAMESPACE);
        }
        fieldBuilder.addOption(DataPacketExtension.NAMESPACE);
        form.addField(fieldBuilder.build());
        return form.build();
    }
}
