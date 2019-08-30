/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.iqprivate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.iqprivate.packet.DefaultPrivateData;
import org.jivesoftware.smackx.iqprivate.packet.PrivateData;
import org.jivesoftware.smackx.iqprivate.packet.PrivateDataIQ;
import org.jivesoftware.smackx.iqprivate.provider.PrivateDataProvider;

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
 * <a href="http://www.xmpp.org/extensions/jep-0049.html">XEP-49</a>. Because this is a
 * non-standard protocol, it is subject to change.
 *
 * @author Matt Tucker
 */
public final class PrivateDataManager extends Manager {
    private static final Map<XMPPConnection, PrivateDataManager> instances = new WeakHashMap<XMPPConnection, PrivateDataManager>();

    public static synchronized PrivateDataManager getInstanceFor(XMPPConnection connection) {
        PrivateDataManager privateDataManager = instances.get(connection);
        if (privateDataManager == null) {
            privateDataManager = new PrivateDataManager(connection);
        }
        return privateDataManager;
    }

    /**
     * Map of provider instances.
     */
    private static final Map<QName, PrivateDataProvider> privateDataProviders = new HashMap<>();

    /**
     * Returns the private data provider registered to the specified XML element name and namespace.
     * For example, if a provider was registered to the element name "prefs" and the
     * namespace "http://www.xmppclient.com/prefs", then the following stanza would trigger
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
        QName key = new QName(namespace, elementName);
        return privateDataProviders.get(key);
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
            PrivateDataProvider provider) {
        QName key = new QName(namespace, elementName);
        privateDataProviders.put(key, provider);
    }

    /**
     * Removes a private data provider with the specified element name and namespace.
     *
     * @param elementName The XML element name.
     * @param namespace The XML namespace.
     */
    public static void removePrivateDataProvider(String elementName, String namespace) {
        QName key = new QName(namespace, elementName);
        privateDataProviders.remove(key);
    }

    /**
     * Creates a new private data manager.
     *
     * @param connection an XMPP connection which must have already undergone a
     *      successful login.
     */
    private PrivateDataManager(XMPPConnection connection) {
        super(connection);
        instances.put(connection, this);
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
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public PrivateData getPrivateData(final String elementName, final String namespace) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Create an IQ packet to get the private data.
        IQ privateDataGet = new PrivateDataIQ(elementName, namespace);

        PrivateDataIQ response = connection().createStanzaCollectorAndSend(
                        privateDataGet).nextResultOrThrow();
        return response.getPrivateData();
    }

    /**
     * Sets a private data value. Each chunk of private data is uniquely identified by an
     * element name and namespace pair. If private data has already been set with the
     * element name and namespace, then the new private data will overwrite the old value.
     *
     * @param privateData the private data.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void setPrivateData(final PrivateData privateData) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Create an IQ packet to set the private data.
        IQ privateDataSet = new PrivateDataIQ(privateData);

        connection().createStanzaCollectorAndSend(privateDataSet).nextResultOrThrow();
    }

    private static final PrivateData DUMMY_PRIVATE_DATA = new PrivateData() {
        @Override
        public String getElementName() {
            return "smackDummyPrivateData";
        }

        @Override
        public String getNamespace() {
            return SmackConfiguration.SMACK_URL_STRING;
        }

        @Override
        public CharSequence toXML() {
            return '<' + getElementName() + " xmlns='" + getNamespace() + "'/>";
        }
    };

    /**
     * Check if the service supports private data.
     *
     * @return true if the service supports private data, false otherwise.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @since 4.2
     */
    public boolean isSupported() throws NoResponseException, NotConnectedException,
                    InterruptedException, XMPPErrorException {
        // This is just a primitive hack, since XEP-49 does not specify a way to determine if the
        // service supports it
        try {
            setPrivateData(DUMMY_PRIVATE_DATA);
            return true;
        }
        catch (XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == Condition.service_unavailable) {
                return false;
            }
            else {
                throw e;
            }
        }
    }

    /**
     * An IQ provider to parse IQ results containing private data.
     */
    public static class PrivateDataIQProvider extends IQProvider<PrivateDataIQ> {

        @Override
        public PrivateDataIQ parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                        throws XmlPullParserException, IOException {
            PrivateData privateData = null;
            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
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
                            XmlPullParser.Event event = parser.next();
                            if (event == XmlPullParser.Event.START_ELEMENT) {
                                String name = parser.getName();
                                event = parser.next();
                                if (event == XmlPullParser.Event.TEXT_CHARACTERS) {
                                    String value = parser.getText();
                                    data.setValue(name, value);
                                }
                                else if (event == XmlPullParser.Event.END_ELEMENT) {
                                    // If an empty element, set the value with the empty string.
                                    data.setValue(name, "");
                                }
                            }
                            else if (event == XmlPullParser.Event.END_ELEMENT) {
                                if (parser.getName().equals(elementName)) {
                                    finished = true;
                                }
                            }
                        }
                        privateData = data;
                    }
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getName().equals("query")) {
                        done = true;
                    }
                }
            }
            return new PrivateDataIQ(privateData);
        }
    }
}
