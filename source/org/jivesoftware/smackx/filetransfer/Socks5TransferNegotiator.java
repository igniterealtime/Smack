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
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.Bytestream;
import org.jivesoftware.smackx.packet.Bytestream.StreamHost;
import org.jivesoftware.smackx.packet.Bytestream.StreamHostUsed;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.packet.StreamInitiation;

import java.io.*;
import java.net.*;
import java.util.*;

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

    public static boolean isAllowLocalProxyHost = true;

    private final XMPPConnection connection;

    private List proxies;

    private List streamHosts;
    private ProxyProcess proxyProcess;

    public Socks5TransferNegotiator(final XMPPConnection connection) {
        this.connection = connection;
    }

    /*
      * (non-Javadoc)
      *
      * @see org.jivesoftware.smackx.filetransfer.StreamNegotiator#initiateDownload(org.jivesoftware.smackx.packet.StreamInitiation,
      *      java.io.File)
      */
    public InputStream initiateIncomingStream(StreamInitiation initiation)
            throws XMPPException {
        StreamInitiation response = super.createInitiationAccept(initiation,
                NAMESPACE);

        // establish collector to await response
        PacketCollector collector = connection
                .createPacketCollector(new AndFilter(new FromMatchesFilter(initiation.getFrom()),
                        new BytestreamSIDFilter(initiation.getSessionID())));
        connection.sendPacket(response);

        Bytestream streamHostsInfo = (Bytestream) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (streamHostsInfo == null) {
            throw new XMPPException("No response from file transfer initiator");
        }
        else if (streamHostsInfo.getType().equals(IQ.Type.ERROR)) {
            throw new XMPPException(streamHostsInfo.getError());
        }
        collector.cancel();

        // select appropriate host
        SelectedHostInfo selectedHost = selectHost(streamHostsInfo);

        // send used-host confirmation
        Bytestream streamResponse = createUsedHostConfirmation(
                selectedHost.selectedHost, streamHostsInfo.getFrom(),
                streamHostsInfo.getTo(), streamHostsInfo.getPacketID());
        connection.sendPacket(streamResponse);

        try {
            return selectedHost.establishedSocket.getInputStream();
        }
        catch (IOException e) {
            throw new XMPPException("Error establishing input stream", e);
        }
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
     * @param streamHostsInfo
     * @return
     * @throws XMPPException
     */
    private SelectedHostInfo selectHost(Bytestream streamHostsInfo)
            throws XMPPException {
        Iterator it = streamHostsInfo.getStreamHosts().iterator();
        StreamHost selectedHost = null;
        Socket socket = null;
        while (it.hasNext()) {
            selectedHost = (StreamHost) it.next();

            // establish socket
            try {
                socket = new Socket(selectedHost.getAddress(), selectedHost
                        .getPort());
                establishSOCKS5ConnectionToProxy(socket, createDigest(
                        streamHostsInfo.getSessionID(), streamHostsInfo
                        .getFrom(), streamHostsInfo.getTo()));
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                selectedHost = null;
                socket = null;
                continue;
            }
        }
        if (selectedHost == null || socket == null) {
            throw new XMPPException(
                    "Could not establish socket with any provided host");
        }

        return new SelectedHostInfo(selectedHost, socket);
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
    public OutputStream initiateOutgoingStream(String streamID, String initiator,
            String target) throws XMPPException {
        Socket socket;
        try {
            socket = initBytestreamSocket(streamID, initiator, target);
        }
        catch (Exception e) {
            throw new XMPPException("Error establishing transfer socket", e);
        }

        if (socket != null) {
            try {
                return socket.getOutputStream();
            }
            catch (IOException e) {
                throw new XMPPException("Error establishing output stream", e);
            }
        }
        return null;
    }

    private Socket initBytestreamSocket(final String sessionID,
            String initiator, String target) throws Exception {
        ProxyProcess process;
        try {
            process = establishListeningSocket();
        }
        catch (IOException io) {
            process = null;
        }

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
        Socket conn = waitForUsedHostResponse(sessionID, process, createDigest(
                sessionID, initiator, target), query).establishedSocket;
        cleanupListeningSocket();
        return conn;
    }


    /**
     * Waits for the peer to respond with which host they chose to use.
     *
     * @param sessionID The session id of the stream.
     * @param proxy     The server socket which will listen locally for remote
     *                  connections.
     * @param digest
     * @param query
     * @return
     * @throws XMPPException
     * @throws IOException
     */
    private SelectedHostInfo waitForUsedHostResponse(String sessionID,
            final ProxyProcess proxy, final String digest,
            final Bytestream query) throws XMPPException, IOException {
        SelectedHostInfo info = new SelectedHostInfo();

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(query.getPacketID()));
        connection.sendPacket(query);

        Packet packet = collector.nextResult();
        collector.cancel();
        Bytestream response;
        if (packet instanceof Bytestream) {
            response = (Bytestream) packet;
        }
        else {
            throw new XMPPException("Unexpected response from remote user");
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

            IQ serverResponse = (IQ) collector.nextResult();
            collector.cancel();
            if (!serverResponse.getType().equals(IQ.Type.RESULT)) {
                info.establishedSocket.close();
                return null;
            }
            return info;
        }
    }

    private ProxyProcess establishListeningSocket() throws IOException {
        if (proxyProcess == null) {
            proxyProcess = new ProxyProcess(new ServerSocket(7777));
            proxyProcess.start();
        }
        proxyProcess.addTransfer();
        return proxyProcess;
    }

    private void cleanupListeningSocket() {
        if (proxyProcess == null) {
            return;
        }
        proxyProcess.removeTransfer();
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
     * @param from initiator@host1/foo - The file transfer initiator.
     * @param to target@host2/bar - The file transfer target.
     * @param sid 'mySID' - the unique identifier for this file transfer
     * @param localIP The IP of the local machine if it is being provided, null otherwise.
     * @param port The port of the local mahine if it is being provided, null otherwise.
     * @return Returns the created <b><i>Bytestream</b></i> packet
     */
    private Bytestream createByteStreamInit(final String from, final String to,
            final String sid, final String localIP, final int port) {

        Bytestream bs = new Bytestream();
        bs.setTo(to);
        bs.setFrom(from);
        bs.setSessionID(sid);
        bs.setType(IQ.Type.SET);
        bs.setMode(Bytestream.Mode.TCP);
        if (localIP != null && port > 0) {
            bs.addStreamHost(from, localIP, port);
        }
        if (proxies == null) {
            initProxies();
        }
        if (streamHosts != null) {
            Iterator it = streamHosts.iterator();
            while (it.hasNext()) {
                bs.addStreamHost((StreamHost) it.next());
            }
        }

        return bs;
    }

    private void initProxies() {
        proxies = new ArrayList();
        ServiceDiscoveryManager manager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        DiscoverItems discoItems;
        try {
            discoItems = manager.discoverItems(connection.getServiceName());

            DiscoverItems.Item item;
            DiscoverInfo info;
            DiscoverInfo.Identity identity;

            Iterator it = discoItems.getItems();
            while (it.hasNext()) {
                item = (Item) it.next();
                info = manager.discoverInfo(item.getEntityID());
                Iterator itx = info.getIdentities();
                while (itx.hasNext()) {
                    identity = (Identity) itx.next();
                    if (identity.getCategory().equalsIgnoreCase("proxy")
                            && identity.getType().equalsIgnoreCase(
                            "bytestreams")) {
                        proxies.add(info.getFrom());
                    }
                }
            }
        }
        catch (XMPPException e) {
            return;
        }
        if (proxies.size() > 0) {
            initStreamHosts();
        }

    }

    private void initStreamHosts() {
        List streamHosts = new ArrayList();
        Iterator it = proxies.iterator();
        IQ query;
        PacketCollector collector;
        Bytestream response;
        while (it.hasNext()) {
            String jid = it.next().toString();
            query = new IQ() {
                public String getChildElementXML() {
                    return "<query xmlns=\"http://jabber.org/protocol/bytestreams\"/>";
                }
            };
            query.setType(IQ.Type.GET);
            query.setTo(jid);

            collector = connection.createPacketCollector(new PacketIDFilter(
                    query.getPacketID()));
            connection.sendPacket(query);

            response = (Bytestream) collector.nextResult(SmackConfiguration
                    .getPacketReplyTimeout());
            if (response != null) {
                streamHosts.addAll(response.getStreamHosts());
            }
            collector.cancel();
        }
        this.streamHosts = streamHosts;
    }

    /**
     * Returns the packet to send notification to the stream host to activate
     * the stream.
     *
     * @param sessionID The session ID of the file transfer to activate.
     * @param from
     * @param to        The JID of the stream host
     * @param target    The JID of the file transfer target.
     * @return Returns the packet to send notification to the stream host to
     *         activate the stream.
     */
    private static Bytestream createByteStreamActivate(final String sessionID,
            final String from, final String to, final String target) {
        Bytestream activate = new Bytestream(sessionID);
        activate.setMode(null);
        activate.setToActivate(target);
        activate.setFrom(from);
        activate.setTo(to);
        activate.setType(IQ.Type.SET);
        return activate;
    }

    /**
     * Negotiates the Socks 5 bytestream when the local computer is acting as
     * the proxy.
     *
     * @param connection The socket connection with the peer.
     * @return The SHA-1 digest that is used to uniquely identify the file
     *         transfer.
     * @throws XMPPException
     * @throws IOException
     */
    private String establishSocks5UploadConnection(Socket connection) throws XMPPException, IOException {
        OutputStream out = new DataOutputStream(connection.getOutputStream());
        InputStream in = new DataInputStream(connection.getInputStream());

        // first byte is version should be 5
        int b = in.read();
        if (b != 5) {
            throw new XMPPException("Only SOCKS5 supported");
        }

        // second byte number of authentication methods supported
        b = in.read();
        int[] auth = new int[b];
        for (int i = 0; i < b; i++) {
            auth[i] = in.read();
        }

        int authMethod = -1;
        for (int i = 0; i < auth.length; i++) {
            authMethod = (auth[i] == 0 ? 0 : -1); // only auth method
            // 0, no
            // authentication,
            // supported
            if (authMethod == 0) {
                break;
            }
        }
        if (authMethod != 0) {
            throw new XMPPException("Authentication method not supported");
        }
        byte[] cmd = new byte[2];
        cmd[0] = (byte) 0x05;
        cmd[1] = (byte) 0x00;
        out.write(cmd);

        String responseDigest = createIncomingSocks5Message(in);
        cmd = createOutgoingSocks5Message(0, responseDigest);

        if (!connection.isConnected()) {
            throw new XMPPException("Socket closed by remote user");
        }
        out.write(cmd);
        return responseDigest;
    }

    public String getNamespace() {
        return NAMESPACE;
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

    private String createIncomingSocks5Message(InputStream in)
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

    private byte[] createOutgoingSocks5Message(int cmd, String digest) {
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

    private class SelectedHostInfo {

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

    private class ProxyProcess implements Runnable {

        private ServerSocket listeningSocket;

        private Map connectionMap = new HashMap();

        private boolean done = false;

        private Thread thread;
        private int transfers;

        public void run() {
            try {
                listeningSocket.setSoTimeout(10000);
            }
            catch (SocketException e) {
                e.printStackTrace();
            }
            while (!done) {
                Socket conn = null;
                synchronized (ProxyProcess.this) {
                    while (transfers <= 0) {
                        transfers = -1;
                        try {
                            ProxyProcess.this.wait();
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
                try {
                    synchronized (listeningSocket) {
                        conn = listeningSocket.accept();
                    }
                    if (conn == null) {
                        continue;
                    }
                    String digest = establishSocks5UploadConnection(conn);
                    synchronized (connectionMap) {
                        connectionMap.put(digest, conn);
                    }
                }
                catch (IOException e) {
                }
                catch (XMPPException e) {
                    e.printStackTrace();
                    if (conn != null) {
                        try {
                            conn.close();
                        }
                        catch (IOException e1) {
                        }
                    }
                }
            }
        }


        public void start() {
            thread.start();
        }

        public void stop() {
            done = true;
        }

        public int getPort() {
            return listeningSocket.getLocalPort();
        }

        ProxyProcess(ServerSocket listeningSocket) {
            thread = new Thread(this, "File Transfer Connection Listener");
            this.listeningSocket = listeningSocket;
        }

        public Socket getSocket(String digest) {
            synchronized (connectionMap) {
                return (Socket) connectionMap.get(digest);
            }
        }

        public void addTransfer() {
            synchronized (this) {
                if (transfers == -1) {
                    transfers = 1;
                    thread.notify();
                }
                else {
                    transfers++;
                }
            }
        }

        public void removeTransfer() {
            synchronized (this) {
                transfers--;
            }
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
