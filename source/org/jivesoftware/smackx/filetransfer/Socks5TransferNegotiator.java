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
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.Bytestream;
import org.jivesoftware.smackx.packet.Bytestream.StreamHost;
import org.jivesoftware.smackx.packet.Bytestream.StreamHostUsed;
import org.jivesoftware.smackx.packet.StreamInitiation;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

/**
 * A SOCKS5 bytestream is negotiated partly over the XMPP XML stream and partly
 * over a seperate socket. The actual transfer though takes place over a
 * seperatly created socket.
 * <p/>
 * A SOCKS5 file transfer generally has three parites, the initiator, the
 * target, and the stream host. The stream host is a specialized SOCKS5 proxy
 * setup on the server, or, the Initiator can act as the Stream Host if the
 * proxy is not available.
 * <p/>
 * The advantage of having a seperate proxy over directly connecting to
 * eachother is if the Initator and the Target are not on the same LAN and are
 * operating behind NAT, the proxy allows for a common location for both parties
 * to connect to and transfer the file.
 * <p/>
 * Smack will attempt to automatically discover any proxies present on your
 * server. If any are detected they will be forwarded to any user attempting to
 * recieve files from you.
 *
 * @author Alexander Wenckus
 * @see <a href="http://www.jabber.org/jeps/jep-0065.html">JEP-0065: SOCKS5
 *      Bytestreams</a>
 */
public class Socks5TransferNegotiator extends StreamNegotiator {

    protected static final String NAMESPACE = "http://jabber.org/protocol/bytestreams";

    /**
     * The number of connection failures it takes to a streamhost for that particular streamhost
     * to be blacklisted. When a host is blacklisted no more connection attempts will be made to
     * it for a period of 2 hours.
     */
    private static final int CONNECT_FAILURE_THRESHOLD = 2;

    public static boolean isAllowLocalProxyHost = true;

    private final XMPPConnection connection;

    private Socks5TransferNegotiatorManager transferNegotiatorManager;

    public Socks5TransferNegotiator(Socks5TransferNegotiatorManager transferNegotiatorManager,
            final XMPPConnection connection)
    {
        this.connection = connection;
        this.transferNegotiatorManager = transferNegotiatorManager;
    }

    public PacketFilter getInitiationPacketFilter(String from, String sessionID) {
        return new AndFilter(new FromMatchesFilter(from),
                new BytestreamSIDFilter(sessionID));
    }

    /*
      * (non-Javadoc)
      *
      * @see org.jivesoftware.smackx.filetransfer.StreamNegotiator#initiateDownload(
      * org.jivesoftware.smackx.packet.StreamInitiation, java.io.File)
      */
    InputStream negotiateIncomingStream(Packet streamInitiation)
            throws XMPPException {
        Bytestream streamHostsInfo = (Bytestream) streamInitiation;

        if (streamHostsInfo.getType().equals(IQ.Type.ERROR)) {
            throw new XMPPException(streamHostsInfo.getError());
        }
        SelectedHostInfo selectedHost;
        try {
            // select appropriate host
            selectedHost = selectHost(streamHostsInfo);
        }
        catch (XMPPException ex) {
            if (ex.getXMPPError() != null) {
                IQ errorPacket = super.createError(streamHostsInfo.getTo(),
                        streamHostsInfo.getFrom(), streamHostsInfo.getPacketID(),
                        ex.getXMPPError());
                connection.sendPacket(errorPacket);
            }
            throw (ex);
        }

        // send used-host confirmation
        Bytestream streamResponse = createUsedHostConfirmation(
                selectedHost.selectedHost, streamHostsInfo.getFrom(),
                streamHostsInfo.getTo(), streamHostsInfo.getPacketID());
        connection.sendPacket(streamResponse);

        try {
            PushbackInputStream stream = new PushbackInputStream(
                    selectedHost.establishedSocket.getInputStream());
            int firstByte = stream.read();
            stream.unread(firstByte);
            return stream;
        }
        catch (IOException e) {
            throw new XMPPException("Error establishing input stream", e);
        }

    }

    public InputStream createIncomingStream(StreamInitiation initiation) throws XMPPException {
        Packet streamInitiation = initiateIncomingStream(connection, initiation);
        return negotiateIncomingStream(streamInitiation);
    }

    /**
     * The used host confirmation is sent to the initiator to indicate to them
     * which of the hosts they provided has been selected and successfully
     * connected to.
     *
     * @param selectedHost The selected stream host.
     * @param initiator    The initiator of the stream.
     * @param target       The target of the stream.
     * @param packetID     The of the packet being responded to.
     * @return The packet that was created to send to the initiator.
     */
    private Bytestream createUsedHostConfirmation(StreamHost selectedHost,
            String initiator, String target, String packetID) {
        Bytestream streamResponse = new Bytestream();
        streamResponse.setTo(initiator);
        streamResponse.setFrom(target);
        streamResponse.setType(IQ.Type.RESULT);
        streamResponse.setPacketID(packetID);
        streamResponse.setUsedHost(selectedHost.getJID());
        return streamResponse;
    }

    /**
     * Selects a host to connect to over which the file will be transmitted.
     *
     * @param streamHostsInfo the packet recieved from the initiator containing the available hosts
     *                        to transfer the file
     * @return the selected host and socket that were created.
     * @throws XMPPException when there is no appropriate host.
     */
    private SelectedHostInfo selectHost(Bytestream streamHostsInfo)
            throws XMPPException {
        Iterator it = streamHostsInfo.getStreamHosts().iterator();
        StreamHost selectedHost = null;
        Socket socket = null;
        while (it.hasNext()) {
            selectedHost = (StreamHost) it.next();
            String address = selectedHost.getAddress();

            // Check to see if this address has been blacklisted
            int failures = getConnectionFailures(address);
            if (failures >= CONNECT_FAILURE_THRESHOLD) {
                continue;
            }
            // establish socket
            try {
                socket = new Socket(address, selectedHost
                        .getPort());
                establishSOCKS5ConnectionToProxy(socket, createDigest(
                        streamHostsInfo.getSessionID(), streamHostsInfo
                        .getFrom(), streamHostsInfo.getTo()));
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                incrementConnectionFailures(address);
                selectedHost = null;
                socket = null;
            }
        }
        if (selectedHost == null || socket == null || !socket.isConnected()) {
            String errorMessage = "Could not establish socket with any provided host";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.no_acceptable, errorMessage));
        }

        return new SelectedHostInfo(selectedHost, socket);
    }

    private void incrementConnectionFailures(String address) {
        transferNegotiatorManager.incrementConnectionFailures(address);
    }

    private int getConnectionFailures(String address) {
        return transferNegotiatorManager.getConnectionFailures(address);
    }

    /**
     * Creates the digest needed for a byte stream. It is the SHA1(sessionID +
     * initiator + target).
     *
     * @param sessionID The sessionID of the stream negotiation
     * @param initiator The inititator of the stream negotiation
     * @param target    The target of the stream negotiation
     * @return SHA-1 hash of the three parameters
     */
    private String createDigest(final String sessionID, final String initiator,
            final String target) {
        return StringUtils.hash(sessionID + StringUtils.parseName(initiator)
                + "@" + StringUtils.parseServer(initiator) + "/"
                + StringUtils.parseResource(initiator)
                + StringUtils.parseName(target) + "@"
                + StringUtils.parseServer(target) + "/"
                + StringUtils.parseResource(target));
    }

    /*
      * (non-Javadoc)
      *
      * @see org.jivesoftware.smackx.filetransfer.StreamNegotiator#initiateUpload(java.lang.String,
      *      org.jivesoftware.smackx.packet.StreamInitiation, java.io.File)
      */
    public OutputStream createOutgoingStream(String streamID, String initiator,
            String target) throws XMPPException
    {
        Socket socket;
        try {
            socket = initBytestreamSocket(streamID, initiator, target);
        }
        catch (Exception e) {
            throw new XMPPException("Error establishing transfer socket", e);
        }

        if (socket != null) {
            try {
                return new BufferedOutputStream(socket.getOutputStream());
            }
            catch (IOException e) {
                throw new XMPPException("Error establishing output stream", e);
            }
        }
        return null;
    }

    private Socket initBytestreamSocket(final String sessionID,
            String initiator, String target) throws Exception {
        Socks5TransferNegotiatorManager.ProxyProcess process;
        try {
            process = establishListeningSocket();
        }
        catch (IOException io) {
            process = null;
        }

        Socket conn;
        try {
            String localIP;
            try {
                localIP = discoverLocalIP();
            }
            catch (UnknownHostException e1) {
                localIP = null;
            }

            Bytestream query = createByteStreamInit(initiator, target, sessionID,
                    localIP, (process != null ? process.getPort() : 0));

            // if the local host is one of the options we need to wait for the
            // remote connection.
            conn = waitForUsedHostResponse(sessionID, process, createDigest(
                    sessionID, initiator, target), query).establishedSocket;
        }
        finally {
            cleanupListeningSocket();
        }

        return conn;
    }


    /**
     * Waits for the peer to respond with which host they chose to use.
     *
     * @param sessionID The session id of the stream.
     * @param proxy     The server socket which will listen locally for remote
     *                  connections.
     * @param digest    the digest of the userids and the session id
     * @param query     the query which the response is being awaited
     * @return the selected host
     * @throws XMPPException when the response from the peer is an error or doesn't occur
     * @throws IOException   when there is an error establishing the local socket
     */
    private SelectedHostInfo waitForUsedHostResponse(String sessionID,
            final Socks5TransferNegotiatorManager.ProxyProcess proxy, final String digest,
            final Bytestream query) throws XMPPException, IOException
    {
        SelectedHostInfo info = new SelectedHostInfo();

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(query.getPacketID()));
        connection.sendPacket(query);

        Packet packet = collector.nextResult(10000);
        collector.cancel();
        Bytestream response;
        if (packet != null && packet instanceof Bytestream) {
            response = (Bytestream) packet;
        }
        else {
            throw new XMPPException("Unexpected response from remote user");
        }

        // check for an error
        if (response.getType().equals(IQ.Type.ERROR)) {
            throw new XMPPException("Remote client returned error, stream hosts expected",
                    response.getError());
        }

        StreamHostUsed used = response.getUsedHost();
        StreamHost usedHost = query.getStreamHost(used.getJID());
        if (usedHost == null) {
            throw new XMPPException("Remote user responded with unknown host");
        }
        // The local computer is acting as the proxy
        if (used.getJID().equals(query.getFrom())) {
            info.establishedSocket = proxy.getSocket(digest);
            info.selectedHost = usedHost;
            return info;
        }
        else {
            info.establishedSocket = new Socket(usedHost.getAddress(), usedHost
                    .getPort());
            establishSOCKS5ConnectionToProxy(info.establishedSocket, digest);

            Bytestream activate = createByteStreamActivate(sessionID, response
                    .getTo(), usedHost.getJID(), response.getFrom());

            collector = connection.createPacketCollector(new PacketIDFilter(
                    activate.getPacketID()));
            connection.sendPacket(activate);

            IQ serverResponse = (IQ) collector.nextResult(SmackConfiguration
                    .getPacketReplyTimeout());
            collector.cancel();
            if (!serverResponse.getType().equals(IQ.Type.RESULT)) {
                info.establishedSocket.close();
                return null;
            }
            return info;
        }
    }

    private Socks5TransferNegotiatorManager.ProxyProcess establishListeningSocket()
            throws IOException {
        return transferNegotiatorManager.addTransfer();
    }

    private void cleanupListeningSocket() {
        transferNegotiatorManager.removeTransfer();
    }

    private String discoverLocalIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * The bytestream init looks like this:
     * <p/>
     * <pre>
     * &lt;iq type='set'
     *     from='initiator@host1/foo'
     *     to='target@host2/bar'
     *     id='initiate'&gt;
     *   &lt;query xmlns='http://jabber.org/protocol/bytestreams'
     *          sid='mySID'
     * 	 mode='tcp'&gt;
     *     &lt;streamhost
     *         jid='initiator@host1/foo'
     *         host='192.168.4.1'
     *        port='5086'/&gt;
     *     &lt;streamhost
     *         jid='proxy.host3'
     *         host='24.24.24.1'
     *         zeroconf='_jabber.bytestreams'/&gt;
     *   &lt;/query&gt;
     * &lt;/iq&gt;
     * </pre>
     *
     * @param from    initiator@host1/foo - the file transfer initiator.
     * @param to      target@host2/bar - the file transfer target.
     * @param sid     'mySID' - the unique identifier for this file transfer
     * @param localIP the IP of the local machine if it is being provided, null otherwise.
     * @param port    the port of the local mahine if it is being provided, null otherwise.
     * @return the created <b><i>Bytestream</b></i> packet
     */
    private Bytestream createByteStreamInit(final String from, final String to,
            final String sid, final String localIP, final int port)
    {
        Bytestream bs = new Bytestream();
        bs.setTo(to);
        bs.setFrom(from);
        bs.setSessionID(sid);
        bs.setType(IQ.Type.SET);
        bs.setMode(Bytestream.Mode.tcp);
        if (localIP != null && port > 0) {
            bs.addStreamHost(from, localIP, port);
        }
        // make sure the proxies have been initialized completely
        Collection<Bytestream.StreamHost> streamHosts = transferNegotiatorManager.getStreamHosts();

        if (streamHosts != null) {
            for (StreamHost host : streamHosts) {
                bs.addStreamHost(host);
            }
        }

        return bs;
    }


    /**
     * Returns the packet to send notification to the stream host to activate
     * the stream.
     *
     * @param sessionID the session ID of the file transfer to activate.
     * @param from      the sender of the bytestreeam
     * @param to        the JID of the stream host
     * @param target    the JID of the file transfer target.
     * @return the packet to send notification to the stream host to
     *         activate the stream.
     */
    private static Bytestream createByteStreamActivate(final String sessionID,
            final String from, final String to, final String target)
    {
        Bytestream activate = new Bytestream(sessionID);
        activate.setMode(null);
        activate.setToActivate(target);
        activate.setFrom(from);
        activate.setTo(to);
        activate.setType(IQ.Type.SET);
        return activate;
    }

    public String[] getNamespaces() {
        return new String[]{NAMESPACE};
    }

    private void establishSOCKS5ConnectionToProxy(Socket socket, String digest)
            throws IOException {

        byte[] cmd = new byte[3];

        cmd[0] = (byte) 0x05;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) 0x00;

        OutputStream out = new DataOutputStream(socket.getOutputStream());
        out.write(cmd);

        InputStream in = new DataInputStream(socket.getInputStream());
        byte[] response = new byte[2];

        in.read(response);

        cmd = createOutgoingSocks5Message(1, digest);
        out.write(cmd);
        createIncomingSocks5Message(in);
    }

    static String createIncomingSocks5Message(InputStream in)
            throws IOException {
        byte[] cmd = new byte[5];
        in.read(cmd, 0, 5);

        byte[] addr = new byte[cmd[4]];
        in.read(addr, 0, addr.length);
        String digest = new String(addr);
        in.read();
        in.read();

        return digest;
    }

    static byte[] createOutgoingSocks5Message(int cmd, String digest) {
        byte addr[] = digest.getBytes();

        byte[] data = new byte[7 + addr.length];
        data[0] = (byte) 5;
        data[1] = (byte) cmd;
        data[2] = (byte) 0;
        data[3] = (byte) 0x3;
        data[4] = (byte) addr.length;

        System.arraycopy(addr, 0, data, 5, addr.length);
        data[data.length - 2] = (byte) 0;
        data[data.length - 1] = (byte) 0;

        return data;
    }

    public void cleanup() {

    }

    private static class SelectedHostInfo {

        protected XMPPException exception;

        protected StreamHost selectedHost;

        protected Socket establishedSocket;

        SelectedHostInfo(StreamHost selectedHost, Socket establishedSocket) {
            this.selectedHost = selectedHost;
            this.establishedSocket = establishedSocket;
        }

        public SelectedHostInfo() {
        }
    }


    private static class BytestreamSIDFilter implements PacketFilter {

        private String sessionID;

        public BytestreamSIDFilter(String sessionID) {
            if (sessionID == null) {
                throw new IllegalArgumentException("StreamID cannot be null");
            }
            this.sessionID = sessionID;
        }

        public boolean accept(Packet packet) {
            if (!Bytestream.class.isInstance(packet)) {
                return false;
            }
            Bytestream bytestream = (Bytestream) packet;
            String sessionID = bytestream.getSessionID();

            return (sessionID != null && sessionID.equals(this.sessionID));
        }
    }
}
