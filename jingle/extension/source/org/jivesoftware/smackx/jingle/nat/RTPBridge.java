/**
 * $RCSfile: RTPBridge.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smackx.jingle.nat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.SmackLogger;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.xmlpull.v1.XmlPullParser;

/**
 * RTPBridge IQ Packet used to request and retrieve a RTPBridge Candidates that can be used for a Jingle Media Transmission between two parties that are behind NAT.
 * This Jingle Bridge has all the needed information to establish a full UDP Channel (Send and Receive) between two parties.
 * <i>This transport method should be used only if other transport methods are not allowed. Or if you want a more reliable transport.</i>
 * <p/>
 * High Level Usage Example:
 * <p/>
 * RTPBridge rtpBridge = RTPBridge.getRTPBridge(connection, sessionID);
 *
 * @author Thiago Camargo
 */
public class RTPBridge extends IQ {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(RTPBridge.class);

	private String sid;
    private String pass;
    private String ip;
    private String name;
    private int portA = -1;
    private int portB = -1;
    private String hostA;
    private String hostB;
    private BridgeAction bridgeAction = BridgeAction.create;

    private enum BridgeAction {

        create, change, publicip
    }

    /**
     * Element name of the packet extension.
     */
    public static final String NAME = "rtpbridge";

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "rtpbridge";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://www.jivesoftware.com/protocol/rtpbridge";

    static {
        ProviderManager.getInstance().addIQProvider(NAME, NAMESPACE, new Provider());
    }

    /**
     * Creates a RTPBridge Instance with defined Session ID
     *
     * @param sid
     */
    public RTPBridge(String sid) {
        this.sid = sid;
    }

    /**
     * Creates a RTPBridge Instance with defined Session ID
     *
     * @param action
     */
    public RTPBridge(BridgeAction action) {
        this.bridgeAction = action;
    }

    /**
     * Creates a RTPBridge Instance with defined Session ID
     *
     * @param sid
     * @param bridgeAction
     */
    public RTPBridge(String sid, BridgeAction bridgeAction) {
        this.sid = sid;
        this.bridgeAction = bridgeAction;
    }

    /**
     * Creates a RTPBridge Packet without Session ID
     */
    public RTPBridge() {
    }

    /**
     * Get the attributes string
     */
    public String getAttributes() {
        StringBuilder str = new StringBuilder();

        if (getSid() != null)
            str.append(" sid='").append(getSid()).append("'");

        if (getPass() != null)
            str.append(" pass='").append(getPass()).append("'");

        if (getPortA() != -1)
            str.append(" porta='").append(getPortA()).append("'");

        if (getPortB() != -1)
            str.append(" portb='").append(getPortB()).append("'");

        if (getHostA() != null)
            str.append(" hosta='").append(getHostA()).append("'");

        if (getHostB() != null)
            str.append(" hostb='").append(getHostB()).append("'");

        return str.toString();
    }

    /**
     * Get the Session ID of the Packet (usually same as Jingle Session ID)
     *
     * @return
     */
    public String getSid() {
        return sid;
    }

    /**
     * Set the Session ID of the Packet (usually same as Jingle Session ID)
     *
     * @param sid
     */
    public void setSid(String sid) {
        this.sid = sid;
    }

    /**
     * Get the Host A IP Address
     *
     * @return
     */
    public String getHostA() {
        return hostA;
    }

    /**
     * Set the Host A IP Address
     *
     * @param hostA
     */
    public void setHostA(String hostA) {
        this.hostA = hostA;
    }

    /**
     * Get the Host B IP Address
     *
     * @return
     */
    public String getHostB() {
        return hostB;
    }

    /**
     * Set the Host B IP Address
     *
     * @param hostB
     */
    public void setHostB(String hostB) {
        this.hostB = hostB;
    }

    /**
     * Get Side A receive port
     *
     * @return
     */
    public int getPortA() {
        return portA;
    }

    /**
     * Set Side A receive port
     *
     * @param portA
     */
    public void setPortA(int portA) {
        this.portA = portA;
    }

    /**
     * Get Side B receive port
     *
     * @return
     */
    public int getPortB() {
        return portB;
    }

    /**
     * Set Side B receive port
     *
     * @param portB
     */
    public void setPortB(int portB) {
        this.portB = portB;
    }

    /**
     * Get the RTP Bridge IP
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * Set the RTP Bridge IP
     *
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Get the RTP Agent Pass
     *
     * @return
     */
    public String getPass() {
        return pass;
    }

    /**
     * Set the RTP Agent Pass
     *
     * @param pass
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * Get the name of the Candidate
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the Candidate
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the Child Element XML of the Packet
     *
     * @return
     */
    public String getChildElementXML() {
        StringBuilder str = new StringBuilder();
        str.append("<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "' sid='").append(sid).append("'>");

        if (bridgeAction.equals(BridgeAction.create))
            str.append("<candidate/>");
        else if (bridgeAction.equals(BridgeAction.change))
            str.append("<relay ").append(getAttributes()).append(" />");
        else
            str.append("<publicip ").append(getAttributes()).append(" />");

        str.append("</" + ELEMENT_NAME + ">");
        return str.toString();
    }

    /**
     * IQProvider for RTP Bridge packets.
     * Parse receive RTPBridge packet to a RTPBridge instance
     *
     * @author Thiago Rocha
     */
    public static class Provider implements IQProvider {

        public Provider() {
            super();
        }

        public IQ parseIQ(XmlPullParser parser) throws Exception {

            boolean done = false;

            int eventType;
            String elementName;
            String namespace;

            if (!parser.getNamespace().equals(RTPBridge.NAMESPACE))
                throw new Exception("Not a RTP Bridge packet");

            RTPBridge iq = new RTPBridge();

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if (parser.getAttributeName(i).equals("sid"))
                    iq.setSid(parser.getAttributeValue(i));
            }

            // Start processing sub-elements
            while (!done) {
                eventType = parser.next();
                elementName = parser.getName();
                namespace = parser.getNamespace();

                if (eventType == XmlPullParser.START_TAG) {
                    if (elementName.equals("candidate")) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("ip"))
                                iq.setIp(parser.getAttributeValue(i));
                            else if (parser.getAttributeName(i).equals("pass"))
                                iq.setPass(parser.getAttributeValue(i));
                            else if (parser.getAttributeName(i).equals("name"))
                                iq.setName(parser.getAttributeValue(i));
                            else if (parser.getAttributeName(i).equals("porta"))
                                iq.setPortA(Integer.parseInt(parser.getAttributeValue(i)));
                            else if (parser.getAttributeName(i).equals("portb"))
                                iq.setPortB(Integer.parseInt(parser.getAttributeValue(i)));
                        }
                    }
                    else if (elementName.equals("publicip")) {
                        //String p = parser.getAttributeName(0);
                        int x = parser.getAttributeCount();
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("ip"))
                                iq.setIp(parser.getAttributeValue(i));
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(RTPBridge.ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }
            return iq;
        }
    }

    /**
     * Get a new RTPBridge Candidate from the server.
     * If a error occurs or the server don't support RTPBridge Service, null is returned.
     *
     * @param connection
     * @param sessionID
     * @return
     */
    public static RTPBridge getRTPBridge(Connection connection, String sessionID) {

        if (!connection.isConnected()) {
            return null;
        }

        RTPBridge rtpPacket = new RTPBridge(sessionID);
        rtpPacket.setTo(RTPBridge.NAME + "." + connection.getServiceName());

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(rtpPacket.getPacketID()));

        connection.sendPacket(rtpPacket);

        RTPBridge response = (RTPBridge) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();

        return response;
    }

    /**
     * Check if the server support RTPBridge Service.
     *
     * @param connection
     * @return
     */
    public static boolean serviceAvailable(Connection connection) {

        if (!connection.isConnected()) {
            return false;
        }

        LOGGER.debug("Service listing");

        ServiceDiscoveryManager disco = ServiceDiscoveryManager
                .getInstanceFor(connection);
        try {
//            DiscoverItems items = disco.discoverItems(connection.getServiceName());
//            Iterator iter = items.getItems();
//            while (iter.hasNext()) {
//                DiscoverItems.Item item = (DiscoverItems.Item) iter.next();
//                if (item.getEntityID().startsWith("rtpbridge.")) {
//                    return true;
//                }
//            }
            
            DiscoverInfo discoInfo = disco.discoverInfo(connection.getServiceName());
            Iterator<DiscoverInfo.Identity> iter = discoInfo.getIdentities();
            while (iter.hasNext()) {
                DiscoverInfo.Identity identity = iter.next();
                if ((identity.getName() != null) && (identity.getName().startsWith("rtpbridge"))) {
					return true;
				}
            }
       }
        catch (XMPPException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Check if the server support RTPBridge Service.
     *
     * @param connection
     * @return
     */
    public static RTPBridge relaySession(Connection connection, String sessionID, String pass, TransportCandidate proxyCandidate, TransportCandidate localCandidate) {

        if (!connection.isConnected()) {
            return null;
        }

        RTPBridge rtpPacket = new RTPBridge(sessionID, RTPBridge.BridgeAction.change);
        rtpPacket.setTo(RTPBridge.NAME + "." + connection.getServiceName());
        rtpPacket.setType(Type.SET);

        rtpPacket.setPass(pass);
        rtpPacket.setPortA(localCandidate.getPort());
        rtpPacket.setPortB(proxyCandidate.getPort());
        rtpPacket.setHostA(localCandidate.getIp());
        rtpPacket.setHostB(proxyCandidate.getIp());

        // LOGGER.debug("Relayed to: " + candidate.getIp() + ":" + candidate.getPort());

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(rtpPacket.getPacketID()));

        connection.sendPacket(rtpPacket);

        RTPBridge response = (RTPBridge) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();

        return response;
    }

    /**
     * Get Public Address from the Server.
     *
     * @param xmppConnection
     * @return public IP String or null if not found
     */
    public static String getPublicIP(Connection xmppConnection) {

        if (!xmppConnection.isConnected()) {
            return null;
        }

        RTPBridge rtpPacket = new RTPBridge(RTPBridge.BridgeAction.publicip);
        rtpPacket.setTo(RTPBridge.NAME + "." + xmppConnection.getServiceName());
        rtpPacket.setType(Type.SET);

        // LOGGER.debug("Relayed to: " + candidate.getIp() + ":" + candidate.getPort());

        PacketCollector collector = xmppConnection
                .createPacketCollector(new PacketIDFilter(rtpPacket.getPacketID()));

        xmppConnection.sendPacket(rtpPacket);

        RTPBridge response = (RTPBridge) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();

        if(response == null) return null;

        if (response.getIp() == null || response.getIp().equals("")) return null;

        Enumeration<NetworkInterface> ifaces = null;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        while (ifaces!=null&&ifaces.hasMoreElements()) {

            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> iaddresses = iface.getInetAddresses();

            while (iaddresses.hasMoreElements()) {
                InetAddress iaddress = iaddresses.nextElement();
                if (!iaddress.isLoopbackAddress())
                    if (iaddress.getHostAddress().indexOf(response.getIp()) >= 0)
                        return null;

            }
        }

        return response.getIp();
    }

}
