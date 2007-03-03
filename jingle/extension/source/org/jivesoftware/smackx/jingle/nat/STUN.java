/**
 * $RCSfile$
 * $Revision: $
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

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.xmlpull.v1.XmlPullParser;

import java.util.Iterator;

/**
 * STUN IQ Packet used to request and retrieve a STUN server and port to make p2p connections easier. STUN is usually used by Jingle Media Transmission between two parties that are behind NAT.
 * <p/>
 * High Level Usage Example:
 * <p/>
 * STUN stun = STUN.getSTUNServer(xmppConnection);
 *
 * @author Thiago Camargo
 */
public class STUN extends IQ {

    private int port = -1;
    private String host;

    /**
     * Element name of the packet extension.
     */
    public static final String NAME = "stun";

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "query";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "google:jingleinfo";

    static {
        ProviderManager.getInstance().addIQProvider(NAME, NAMESPACE, new STUN.Provider());
    }

    /**
     * Creates a STUN IQ
     */
    public STUN() {
    }

    /**
     * Get the Host Address
     *
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the Host Address
     *
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get STUN port
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Set STUN port
     *
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
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
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("host"))
                                iq.setHost(parser.getAttributeValue(i));
                            else if (parser.getAttributeName(i).equals("udp"))
                                iq.setPort(Integer.parseInt(parser.getAttributeValue(i)));
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
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
     * If a error occurs or the server don´t support STUN Service, null is returned.
     *
     * @param xmppConnection
     * @return
     */
    public static STUN getSTUNServer(XMPPConnection xmppConnection) {

        if (!xmppConnection.isConnected()) {
            return null;
        }

        STUN stunPacket = new STUN();
        stunPacket.setTo(NAME + "." + xmppConnection.getServiceName());

        PacketCollector collector = xmppConnection
                .createPacketCollector(new PacketIDFilter(stunPacket.getPacketID()));

        xmppConnection.sendPacket(stunPacket);

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
    public static boolean serviceAvailable(XMPPConnection xmppConnection) {

        if (!xmppConnection.isConnected()) {
            return false;
        }

        System.out.println("Service listing");

        ServiceDiscoveryManager disco = ServiceDiscoveryManager
                .getInstanceFor(xmppConnection);
        try {
            DiscoverItems items = disco.discoverItems(xmppConnection.getServiceName());

            Iterator iter = items.getItems();
            while (iter.hasNext()) {
                DiscoverItems.Item item = (DiscoverItems.Item) iter.next();
                DiscoverInfo info = disco.discoverInfo(item.getEntityID());

                Iterator<DiscoverInfo.Identity> iter2 = info.getIdentities();
                while (iter2.hasNext()) {
                    DiscoverInfo.Identity identity = iter2.next();
                    if (identity.getCategory().equals("proxy") && identity.getType().equals("stun"))
                        if (info.containsFeature(NAMESPACE))
                            return true;
                }

            }
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
        return false;
    }
}
