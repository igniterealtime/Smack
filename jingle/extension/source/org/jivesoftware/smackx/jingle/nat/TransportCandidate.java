/**
 * $RCSfile: TransportCandidate.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.jingle.nat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.SmackLogger;
import org.jivesoftware.smackx.jingle.nat.TransportResolverListener.Checker;

/**
 * Transport candidate.
 * <p/>
 * A candidate represents the possible transport for data interchange between
 * the two endpoints.
 *
 * @author Thiago Camargo
 * @author Alvaro Saurin
 */
public abstract class TransportCandidate {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(TransportCandidate.class);

	private String name;

    private String ip; // IP address

    private int port; // Port to use, or 0 for any port

    private String localIp;

    private int generation;

    protected String password;

    private String sessionId;

    private Connection connection;

    private TransportCandidate symmetric;

    private CandidateEcho candidateEcho = null;

    private Thread echoThread = null;

    // Listeners for events
    private final List<TransportResolverListener.Checker> listeners = new ArrayList<Checker>();

    public void addCandidateEcho(JingleSession session) throws SocketException, UnknownHostException {
        candidateEcho = new CandidateEcho(this, session);
        echoThread = new Thread(candidateEcho);
        echoThread.start();
    }

    public void removeCandidateEcho() {
        if (candidateEcho != null)
            candidateEcho.cancel();
        candidateEcho = null;
        echoThread = null;
    }

    public CandidateEcho getCandidateEcho() {
        return candidateEcho;
    }

    public String getIp() {
        return ip;
    }

    /**
     * Set the IP address.
     *
     * @param ip the IP address
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Get local IP to bind to this candidate
     *
     * @return
     */
    public String getLocalIp() {
        return localIp == null ? ip : localIp;
    }

    /**
     * Set local IP to bind to this candidate
     *
     * @param localIp
     */
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    /**
     * Get the symetric candidate for this candidate if it exists.
     *
     * @return
     */
    public TransportCandidate getSymmetric() {
        return symmetric;
    }

    /**
     * Set the symetric candidate for this candidate.
     *
     * @param symetric
     */
    public void setSymmetric(TransportCandidate symetric) {
        this.symmetric = symetric;
    }

    /**
     * Get the password used by ICE or relayed candidate
     *
     * @return a password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password used by ICE or relayed candidate
     *
     * @param password a password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the Connection use to send or receive this candidate
     *
     * @return
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Set the Connection use to send or receive this candidate
     *
     * @param connection
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get the jingle's sessionId that is using this candidate
     *
     * @return
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Set the jingle's sessionId that is using this candidate
     *
     * @param sessionId
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Empty constructor
     */
    public TransportCandidate() {
        this(null, 0, 0);
    }

    /**
     * Constructor with IP address and port
     *
     * @param ip   The IP address.
     * @param port The port number.
     */
    public TransportCandidate(String ip, int port) {
        this(ip, port, 0);
    }

    /**
     * Constructor with IP address and port
     *
     * @param ip         The IP address.
     * @param port       The port number.
     * @param generation The generation
     */
    public TransportCandidate(String ip, int port, int generation) {
        this.ip = ip;
        this.port = port;
        this.generation = generation;
    }

    /**
     * Return true if the candidate is not valid.
     *
     * @return true if the candidate is null.
     */
    public boolean isNull() {
        if (ip == null) {
            return true;
        } else if (ip.length() == 0) {
            return true;
        } else if (port < 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the port, or 0 for any port.
     *
     * @return the port or 0
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port, using 0 for any port
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the generation for a transportElement definition
     *
     * @return the generation
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Set the generation for a transportElement definition.
     *
     * @param generation the generation number
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    /**
     * Get the name used for identifying this transportElement method (optional)
     *
     * @return a name used for identifying this transportElement (ie,
     *         "myrtpvoice1")
     */
    public String getName() {
        return name;
    }

    /**
     * Set a name for identifying this transportElement.
     *
     * @param name the name used for the transportElement
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Object#equals(java.lang.Object)
      */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransportCandidate other = (TransportCandidate) obj;
        if (generation != other.generation) {
            return false;
        }
        if (getIp() == null) {
            if (other.getIp() != null) {
                return false;
            }
        } else if (!getIp().equals(other.getIp())) {
            return false;
        }

        if (getPort() != other.getPort()) {
            return false;
        }

        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        if (getPort() != other.getPort()) {
            return false;
        }
        return true;
    }


    /**
     * Check if a transport candidate is usable. The transport resolver should
     * check if the transport candidate the other endpoint has provided is
     * usable.
     * <p/>
     * Subclasses should provide better methods if they can...
     */
    public void check(final List<TransportCandidate> localCandidates) {
        //TODO candidate is being checked trigger
        //candidatesChecking.add(cand);

        Thread checkThread = new Thread(new Runnable() {
            public void run() {
                boolean isUsable;

                
                try {
                	InetAddress candAddress = InetAddress.getByName(getIp());
                    isUsable = true;//candAddress.isReachable(TransportResolver.CHECK_TIMEOUT);
                }
                catch (Exception e) {
                    isUsable = false;
                }
                triggerCandidateChecked(isUsable);

                //TODO candidate is being checked trigger
                //candidatesChecking.remove(cand);
            }
        }, "Transport candidate check");

        checkThread.setName("Transport candidate test");
        checkThread.start();
    }

    /**
     * Trigger a new candidate checked event.
     *
     * @param result The result.
     */
    void triggerCandidateChecked(boolean result) {

        for (TransportResolverListener.Checker trl : getListenersList()) {
            trl.candidateChecked(this, result);
        }
    }

    /**
     * Get the list of listeners
     *
     * @return the list of listeners
     */
    public List<TransportResolverListener.Checker> getListenersList() {
        synchronized (listeners) {
            return new ArrayList<Checker>(listeners);
        }
    }

    /**
     * Add a transport resolver listener.
     *
     * @param li The transport resolver listener to be added.
     */
    public void addListener(TransportResolverListener.Checker li) {
        synchronized (listeners) {
            listeners.add(li);
        }
    }

    /**
     * Fixed transport candidate
     */
    public static class Fixed extends TransportCandidate {

        public Fixed() {
            super();
        }

        /**
         * Constructor with IP address and port
         *
         * @param ip   The IP address.
         * @param port The port number.
         */
        public Fixed(String ip, int port) {
            super(ip, port);
        }

        /**
         * Constructor with IP address and port
         *
         * @param ip         The IP address.
         * @param port       The port number.
         * @param generation The generation
         */
        public Fixed(String ip, int port, int generation) {
            super(ip, port, generation);
        }
    }

    /**
     * Type-safe enum for the transportElement protocol
     */
    public static class Protocol {

        public static final Protocol UDP = new Protocol("udp");

        public static final Protocol TCP = new Protocol("tcp");

        public static final Protocol TCPACT = new Protocol("tcp-act");

        public static final Protocol TCPPASS = new Protocol("tcp-pass");

        public static final Protocol SSLTCP = new Protocol("ssltcp");

        private String value;

        public Protocol(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the Protocol constant associated with the String value.
         */
        public static Protocol fromString(String value) {
            if (value == null) {
                return UDP;
            }
            value = value.toLowerCase();
            if (value.equals("udp")) {
                return UDP;
            } else if (value.equals("tcp")) {
                return TCP;
            } else if (value.equals("tcp-act")) {
                return TCPACT;
            } else if (value.equals("tcp-pass")) {
                return TCPPASS;
            } else if (value.equals("ssltcp")) {
                return SSLTCP;
            } else {
                return UDP;
            }
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Protocol other = (Protocol) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        /**
         * Return true if the protocol is not valid.
         *
         * @return true if the protocol is null
         */
        public boolean isNull() {
            if (value == null) {
                return true;
            } else if (value.length() == 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Type-safe enum for the transportElement channel
     */
    public static class Channel {

        public static final Channel MYRTPVOICE = new Channel("myrtpvoice");

        public static final Channel MYRTCPVOICE = new Channel("myrtcpvoice");

        private String value;

        public Channel(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the MediaChannel constant associated with the String value.
         */
        public static Channel fromString(String value) {
            if (value == null) {
                return MYRTPVOICE;
            }
            value = value.toLowerCase();
            if (value.equals("myrtpvoice")) {
                return MYRTPVOICE;
            } else if (value.equals("tcp")) {
                return MYRTCPVOICE;
            } else {
                return MYRTPVOICE;
            }
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Channel other = (Channel) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        /**
         * Return true if the channel is not valid.
         *
         * @return true if the channel is null
         */
        public boolean isNull() {
            if (value == null) {
                return true;
            } else if (value.length() == 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class CandidateEcho implements Runnable {

        DatagramSocket socket = null;
        String localUser = null;
        String remoteUser = null;
        String id = null;
        byte send[] = null;
        byte receive[] = null;
        DatagramPacket sendPacket = null;
        List<DatagramListener> listeners = new ArrayList<DatagramListener>();
        List<ResultListener> resultListeners = new ArrayList<ResultListener>();
        boolean enabled = true;
        boolean ended = false;
        long replyTries = 2;
        long tries = 10;
        TransportCandidate candidate = null;

        public CandidateEcho(TransportCandidate candidate, JingleSession session) throws UnknownHostException, SocketException {
            this.socket = new DatagramSocket(candidate.getPort(), InetAddress.getByName(candidate.getLocalIp()));
            this.localUser = session.getInitiator();
            this.remoteUser = session.getResponder();
            this.id = session.getSid();
            this.candidate = candidate;

            int keySplitIndex = ((int) Math.ceil(((float) id.length()) / 2));

            String local = id.substring(0, keySplitIndex) + ";" + localUser;
            String remote = id.substring(keySplitIndex) + ";" + remoteUser;

            try {
                if (session.getConnection().getUser().equals(session.getInitiator())) {

                    this.send = local.getBytes("UTF-8");
                    this.receive = remote.getBytes("UTF-8");
                } else {
                    this.receive = local.getBytes("UTF-8");
                    this.send = remote.getBytes("UTF-8");
                }
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }

        public void run() {
            try {
                LOGGER.debug("Listening for ECHO: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());
                while (true) {

                    DatagramPacket packet = new DatagramPacket(new byte[150], 150);

                    socket.receive(packet);

                    //LOGGER.debug("ECHO Packet Received in: " + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort() + " From: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

                    boolean accept = false;

                    ByteBuffer buf = ByteBuffer.wrap(packet.getData());
                    byte[] content = new byte[packet.getLength()];
                    buf = buf.get(content, 0, packet.getLength());

                    packet.setData(content);

                    for (DatagramListener listener : listeners) {
                        accept = listener.datagramReceived(packet);
                        if (accept) break;
                    }

                    long delay = 100 / replyTries;

                    String str[] = new String(packet.getData(), "UTF-8").split(";");
                    String pass = str[0];
                    String address[] = str[1].split(":");
                    String ip = address[0];
                    String port = address[1];

                    if (pass.equals(candidate.getPassword()) && !accept) {

                        byte[] cont = null;
                        try {
                            cont = (password + ";" + candidate.getIp() + ":" + candidate.getPort()).getBytes("UTF-8");
                        }
                        catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        packet.setData(cont);
                        packet.setLength(cont.length);
                        packet.setAddress(InetAddress.getByName(ip));
                        packet.setPort(Integer.parseInt(port));

                        for (int i = 0; i < replyTries; i++) {
                            socket.send(packet);
                            if (!enabled) break;
                            try {
                                Thread.sleep(delay);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            catch (UnknownHostException uhe) {
                if (enabled) {
                }
            }
            catch (SocketException se) {
                if (enabled) {
                }
            }
            catch (IOException ioe) {
                if (enabled) {
                }
            }
            catch (Exception e) {
                if (enabled) {
                }
            }
        }

        public void cancel() {
            this.enabled = false;
            socket.close();
        }

        private void fireTestResult(TestResult testResult, TransportCandidate candidate) {
            for (ResultListener resultListener : resultListeners)
                resultListener.testFinished(testResult, candidate);
        }

        public void testASync(final TransportCandidate transportCandidate, final String password) {

            Thread thread = new Thread(new Runnable() {

                public void run() {

                    DatagramListener listener = new DatagramListener() {
                        public boolean datagramReceived(DatagramPacket datagramPacket) {

                            try {
                                LOGGER.debug("ECHO Received to: " + candidate.getIp() + ":" + candidate.getPort() + "  data: " + new String(datagramPacket.getData(), "UTF-8"));
                                String str[] = new String(datagramPacket.getData(), "UTF-8").split(";");
                                String pass = str[0];
                                String addr[] = str[1].split(":");
                                String ip = addr[0];
                                String pt = addr[1];

                                if (pass.equals(password) 
                                		&& transportCandidate.getIp().indexOf(ip) != -1 
                                		&& transportCandidate.getPort() == Integer.parseInt(pt)) {
                                    LOGGER.debug("ECHO OK: " + candidate.getIp() + ":" + candidate.getPort() + " <-> " + transportCandidate.getIp() + ":" + transportCandidate.getPort());
                                    TestResult testResult = new TestResult();
                                    testResult.setResult(true);
                                    ended = true;
                                    fireTestResult(testResult, transportCandidate);
                                    return true;
                                }

                            }
                            catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            LOGGER.debug("ECHO Wrong Data: " + datagramPacket.getAddress().getHostAddress() + ":" + datagramPacket.getPort());
                            return false;
                        }
                    };

                    addListener(listener);

                    byte[] content = null;
                    try {
                        content = new String(password + ";" + getIp() + ":" + getPort()).getBytes("UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    DatagramPacket packet = new DatagramPacket(content, content.length);

                    try {
                        packet.setAddress(InetAddress.getByName(transportCandidate.getIp()));
                    }
                    catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    packet.setPort(transportCandidate.getPort());

                    long delay = 200;

                    try {
                        for (int i = 0; i < tries; i++) {
                            socket.send(packet);
                            if (ended) break;
                            try {
                                Thread.sleep(delay);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (IOException e) {
                        // Do Nothing
                    }

                    try {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    removeListener(listener);
                }
            });
            thread.start();
        }

        public void addListener(DatagramListener listener) {
            listeners.add(listener);
        }

        public void removeListener(DatagramListener listener) {
            listeners.remove(listener);
        }

        public void addResultListener(ResultListener resultListener) {
            resultListeners.add(resultListener);
        }

        public void removeResultListener(ResultListener resultListener) {
            resultListeners.remove(resultListener);
        }

    }

}
