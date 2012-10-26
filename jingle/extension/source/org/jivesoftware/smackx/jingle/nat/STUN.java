/**
 * $RCSfile: STUN.java,v $
 * $Revision: 1.1 $
 * $Date: 15/11/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.nat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.xmlpull.v1.XmlPullParser;

/**
 * STUN IQ Packet used to request and retrieve a STUN server and port to make p2p connections easier. STUN is usually used by Jingle Media Transmission between two parties that are behind NAT.
 * <p/>
 * High Level Usage Example:
 * <p/>
 * STUN stun = STUN.getSTUNServer(connection);
 *
 * @author Thiago Camargo
 */
public class STUN extends IQ {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(STUN.class);

	private List<StunServerAddress> servers = new ArrayList<StunServerAddress>();

    private String publicIp = null;

    /**
     * Element name of the packet extension.
     */
    public static final String DOMAIN = "stun";

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "query";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "google:jingleinfo";

    static {
        ProviderManager.getInstance().addIQProvider(ELEMENT_NAME, NAMESPACE, new STUN.Provider());
    }

    /**
     * Creates a STUN IQ
     */
    public STUN() {
    }

    /**
     * Get a list of STUN Servers recommended by the Server
     *
     * @return
     */
    public List<StunServerAddress> getServers() {
        return servers;
    }

    /**
     * Get Public Ip returned from the XMPP server
     *
     * @return
     */
    public String getPublicIp() {
        return publicIp;
    }

    /**
     * Set Public Ip returned from the XMPP server
     *
     * @param publicIp
     */
    private void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    /**
     * Get the Child Element XML of the Packet
     *
     * @return
     */
    public String getChildElementXML() {
        StringBuilder str = new StringBuilder();
        str.append("<" + ELEMENT_NAME + " xmlns='" + NAMESPACE + "'/>");
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

            if (!parser.getNamespace().equals(NAMESPACE))
                throw new Exception("Not a STUN packet");

            STUN iq = new STUN();

            // Start processing sub-elements
            while (!done) {
                eventType = parser.next();
                elementName = parser.getName();
                namespace = parser.getNamespace();

                if (eventType == XmlPullParser.START_TAG) {
                    if (elementName.equals("server")) {
                        String host = null;
                        String port = null;
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("host"))
                                host = parser.getAttributeValue(i);
                            else if (parser.getAttributeName(i).equals("udp"))
                                port = parser.getAttributeValue(i);
                        }
                        if (host != null && port != null)
                            iq.servers.add(new StunServerAddress(host, port));
                    }
                    else if (elementName.equals("publicip")) {
                        String host = null;
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("ip"))
                                host = parser.getAttributeValue(i);
                        }
                        if (host != null && !host.equals(""))
                            iq.setPublicIp(host);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }
            return iq;
        }
    }

    /**
     * Get a new STUN Server Address and port from the server.
     * If a error occurs or the server don't support STUN Service, null is returned.
     *
     * @param connection
     * @return
     */
    public static STUN getSTUNServer(Connection connection) {

        if (!connection.isConnected()) {
            return null;
        }

        STUN stunPacket = new STUN();
        stunPacket.setTo(DOMAIN + "." + connection.getServiceName());

        PacketCollector collector = connection
                .createPacketCollector(new PacketIDFilter(stunPacket.getPacketID()));

        connection.sendPacket(stunPacket);

        STUN response = (STUN) collector
                .nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();

        return response;
    }

    /**
     * Check if the server support STUN Service.
     *
     * @param xmppConnection
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
            DiscoverItems items = disco.discoverItems(connection.getServiceName());

            Iterator<DiscoverItems.Item> iter = items.getItems();
            while (iter.hasNext()) {
                DiscoverItems.Item item = iter.next();
                DiscoverInfo info = disco.discoverInfo(item.getEntityID());

                Iterator<DiscoverInfo.Identity> iter2 = info.getIdentities();
                while (iter2.hasNext()) {
                    DiscoverInfo.Identity identity = iter2.next();
                    if (identity.getCategory().equals("proxy") && identity.getType().equals("stun"))
                        if (info.containsFeature(NAMESPACE))
                            return true;
                }

                LOGGER.debug(item.getName()+"-"+info.getType());

            }
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Provides easy abstract to store STUN Server Addresses and Ports
     */
    public static class StunServerAddress {

        private String server;
        private String port;

        public StunServerAddress(String server, String port) {
            this.server = server;
            this.port = port;
        }

        /**
         * Get the Host Address
         *
         * @return
         */
        public String getServer() {
            return server;
        }

        /**
         * Get the Server Port
         *
         * @return
         */
        public String getPort() {
            return port;
        }
    }
}
