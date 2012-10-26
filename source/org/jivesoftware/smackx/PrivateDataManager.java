/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.DefaultPrivateData;
import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.Hashtable;
import java.util.Map;

/**
 * Manages private data, which is a mechanism to allow users to store arbitrary XML
 * data on an XMPP server. Each private data chunk is defined by a element name and
 * XML namespace. Example private data:
 *
 * <pre>
 * &lt;color xmlns="http://example.com/xmpp/color"&gt;
 *     &lt;favorite&gt;blue&lt;/blue&gt;
 *     &lt;leastFavorite&gt;puce&lt;/leastFavorite&gt;
 * &lt;/color&gt;
 * </pre>
 *
 * {@link PrivateDataProvider} instances are responsible for translating the XML into objects.
 * If no PrivateDataProvider is registered for a given element name and namespace, then
 * a {@link DefaultPrivateData} instance will be returned.<p>
 *
 * Warning: this is an non-standard protocol documented by
 * <a href="http://www.jabber.org/jeps/jep-0049.html">JEP-49</a>. Because this is a
 * non-standard protocol, it is subject to change.
 *
 * @author Matt Tucker
 */
public class PrivateDataManager {

    /**
     * Map of provider instances.
     */
    private static Map<String, PrivateDataProvider> privateDataProviders = new Hashtable<String, PrivateDataProvider>();

    /**
     * Returns the private data provider registered to the specified XML element name and namespace.
     * For example, if a provider was registered to the element name "prefs" and the
     * namespace "http://www.xmppclient.com/prefs", then the following packet would trigger
     * the provider:
     *
     * <pre>
     * &lt;iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'&gt;
     *     &lt;query xmlns='jabber:iq:private'&gt;
     *         &lt;prefs xmlns='http://www.xmppclient.com/prefs'&gt;
     *             &lt;value1&gt;ABC&lt;/value1&gt;
     *             &lt;value2&gt;XYZ&lt;/value2&gt;
     *         &lt;/prefs&gt;
     *     &lt;/query&gt;
     * &lt;/iq&gt;</pre>
     *
     * <p>Note: this method is generally only called by the internal Smack classes.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @return the PrivateData provider.
     */
    public static PrivateDataProvider getPrivateDataProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return (PrivateDataProvider)privateDataProviders.get(key);
    }

    /**
     * Adds a private data provider with the specified element name and name space. The provider
     * will override any providers loaded through the classpath.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the private data provider.
     */
    public static void addPrivateDataProvider(String elementName, String namespace,
            PrivateDataProvider provider)
    {
        String key = getProviderKey(elementName, namespace);
        privateDataProviders.put(key, provider);
    }

    /**
     * Removes a private data provider with the specified element name and namespace.
     *
     * @param elementName The XML element name.
     * @param namespace The XML namespace.
     */
    public static void removePrivateDataProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        privateDataProviders.remove(key);
    }


    private Connection connection;

    /**
     * The user to get and set private data for. In most cases, this value should
     * be <tt>null</tt>, as the typical use of private data is to get and set
     * your own private data and not others.
     */
    private String user;

    /**
     * Creates a new private data manager. The connection must have
     * undergone a successful login before being used to construct an instance of
     * this class.
     *
     * @param connection an XMPP connection which must have already undergone a
     *      successful login.
     */
    public PrivateDataManager(Connection connection) {
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must be logged in to XMPP server.");
        }
        this.connection = connection;
    }

    /**
     * Creates a new private data manager for a specific user (special case). Most
     * servers only support getting and setting private data for the user that
     * authenticated via the connection. However, some servers support the ability
     * to get and set private data for other users (for example, if you are the
     * administrator). The connection must have undergone a successful login before
     * being used to construct an instance of this class.
     *
     * @param connection an XMPP connection which must have already undergone a
     *      successful login.
     * @param user the XMPP address of the user to get and set private data for.
     */
    public PrivateDataManager(Connection connection, String user) {
        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("Must be logged in to XMPP server.");
        }
        this.connection = connection;
        this.user = user;
    }

    /**
     * Returns the private data specified by the given element name and namespace. Each chunk
     * of private data is uniquely identified by an element name and namespace pair.<p>
     *
     * If a PrivateDataProvider is registered for the specified element name/namespace pair then
     * that provider will determine the specific object type that is returned. If no provider
     * is registered, a {@link DefaultPrivateData} instance will be returned.
     *
     * @param elementName the element name.
     * @param namespace the namespace.
     * @return the private data.
     * @throws XMPPException if an error occurs getting the private data.
     */
    public PrivateData getPrivateData(final String elementName, final String namespace)
            throws XMPPException
    {
        // Create an IQ packet to get the private data.
        IQ privateDataGet = new IQ() {
            public String getChildElementXML() {
                StringBuilder buf = new StringBuilder();
                buf.append("<query xmlns=\"jabber:iq:private\">");
                buf.append("<").append(elementName).append(" xmlns=\"").append(namespace).append("\"/>");
                buf.append("</query>");
                return buf.toString();
            }
        };
        privateDataGet.setType(IQ.Type.GET);
        // Address the packet to the other account if user has been set.
        if (user != null) {
            privateDataGet.setTo(user);
        }

        // Setup a listener for the reply to the set operation.
        String packetID = privateDataGet.getPacketID();
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(packetID));

        // Send the private data.
        connection.sendPacket(privateDataGet);

        // Wait up to five seconds for a response from the server.
        IQ response = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        return ((PrivateDataResult)response).getPrivateData();
    }

    /**
     * Sets a private data value. Each chunk of private data is uniquely identified by an
     * element name and namespace pair. If private data has already been set with the
     * element name and namespace, then the new private data will overwrite the old value.
     *
     * @param privateData the private data.
     * @throws XMPPException if setting the private data fails.
     */
    public void setPrivateData(final PrivateData privateData) throws XMPPException {
        // Create an IQ packet to set the private data.
        IQ privateDataSet = new IQ() {
            public String getChildElementXML() {
                StringBuilder buf = new StringBuilder();
                buf.append("<query xmlns=\"jabber:iq:private\">");
                buf.append(privateData.toXML());
                buf.append("</query>");
                return buf.toString();
            }
        };
        privateDataSet.setType(IQ.Type.SET);
        // Address the packet to the other account if user has been set.
        if (user != null) {
            privateDataSet.setTo(user);
        }

        // Setup a listener for the reply to the set operation.
        String packetID = privateDataSet.getPacketID();
        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(packetID));

        // Send the private data.
        connection.sendPacket(privateDataSet);

        // Wait up to five seconds for a response from the server.
        IQ response = (IQ)collector.nextResult(5000);
        // Stop queuing results
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
    }

    /**
     * Returns a String key for a given element name and namespace.
     *
     * @param elementName the element name.
     * @param namespace the namespace.
     * @return a unique key for the element name and namespace pair.
     */
    private static String getProviderKey(String elementName, String namespace) {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(elementName).append("/><").append(namespace).append("/>");
        return buf.toString();
    }

    /**
     * An IQ provider to parse IQ results containing private data.
     */
    public static class PrivateDataIQProvider implements IQProvider {
        public IQ parseIQ(XmlPullParser parser) throws Exception {
            PrivateData privateData = null;
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    // See if any objects are registered to handle this private data type.
                    PrivateDataProvider provider = getPrivateDataProvider(elementName, namespace);
                    // If there is a registered provider, use it.
                    if (provider != null) {
                        privateData = provider.parsePrivateData(parser);
                    }
                    // Otherwise, use a DefaultPrivateData instance to store the private data.
                    else {
                        DefaultPrivateData data = new DefaultPrivateData(elementName, namespace);
                        boolean finished = false;
                        while (!finished) {
                            int event = parser.next();
                            if (event == XmlPullParser.START_TAG) {
                                String name = parser.getName();
                                // If an empty element, set the value with the empty string.
                                if (parser.isEmptyElementTag()) {
                                    data.setValue(name,"");
                                }
                                // Otherwise, get the the element text.
                                else {
                                    event = parser.next();
                                    if (event == XmlPullParser.TEXT) {
                                        String value = parser.getText();
                                        data.setValue(name, value);
                                    }
                                }
                            }
                            else if (event == XmlPullParser.END_TAG) {
                                if (parser.getName().equals(elementName)) {
                                    finished = true;
                                }
                            }
                        }
                        privateData = data;
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("query")) {
                        done = true;
                    }
                }
            }
            return new PrivateDataResult(privateData);
        }
    }

    /**
     * An IQ packet to hold PrivateData GET results.
     */
    private static class PrivateDataResult extends IQ {

        private PrivateData privateData;

        PrivateDataResult(PrivateData privateData) {
            this.privateData = privateData;
        }

        public PrivateData getPrivateData() {
            return privateData;
        }

        public String getChildElementXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<query xmlns=\"jabber:iq:private\">");
            if (privateData != null) {
                privateData.toXML();
            }
            buf.append("</query>");
            return buf.toString();
        }
    }
}