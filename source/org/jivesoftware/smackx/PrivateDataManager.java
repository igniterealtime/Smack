/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.provider.*;
import org.xmlpull.v1.XmlPullParser;

import java.util.Map;
import java.util.Hashtable;

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
    private static Map privateDataProviders = new Hashtable();

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


    private XMPPConnection connection;

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
    public PrivateDataManager(XMPPConnection connection) {
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
    public PrivateDataManager(XMPPConnection connection, String user) {
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
                StringBuffer buf = new StringBuffer();
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
                StringBuffer buf = new StringBuffer();
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
        StringBuffer buf = new StringBuffer();
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
            IQ result = new PrivateDataResult(privateData);
            return result;
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
            StringBuffer buf = new StringBuffer();
            buf.append("<query xmlns=\"jabber:iq:private\">");
            if (privateData != null) {
                privateData.toXML();
            }
            buf.append("</query>");
            return buf.toString();
        }
    }
}