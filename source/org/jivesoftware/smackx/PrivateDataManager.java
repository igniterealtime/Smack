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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.packet.DefaultPrivateData;
import org.jivesoftware.smackx.provider.*;
import org.xmlpull.v1.XmlPullParser;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.Hashtable;

/**
 * Manages private data.
 *
 * @author Matt Tucker
 */
public class PrivateDataManager {

    private static Map privateDataProviders = new Hashtable();

    private XMPPConnection con;
    private String user;

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
     * @return the IQ provider.
     */
    public static Object getPrivateDataProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return privateDataProviders.get(key);
    }

    /**
     * Adds a private data provider with the specified element name and name space. The provider
     * will override any providers loaded through the classpath.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the IQ provider.
     */
    public static void addPrivateDataProvider(String elementName, String namespace,
            IQProvider provider)
    {
        String key = getProviderKey(elementName, namespace);
        privateDataProviders.put(key, provider);
    }

    /**
     * Creates a new private data manager.
     *
     * @param con an XMPPConnection.
     */
    public PrivateDataManager(XMPPConnection con) {
        this.con = con;
        this.user = con.getUser();
    }

    /**
     * Returns the private data specified by the given element name and namespace.
     *
     * @param elementName the element name.
     * @param namespace the namespace.
     * @return the private data.
     * @throws XMPPException
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

        // Setup a listener for the reply to the set operation.
        String packetID = privateDataGet.getPacketID();
        PacketCollector collector = con.createPacketCollector(new PacketIDFilter(packetID));

        // Send the private data.
        con.sendPacket(privateDataGet);

        // Wait up to five seconds for a response from the server.
        IQ response = (IQ)collector.nextResult(5000);
        if (response == null) {
            throw new XMPPException("No response from the server.");
        }
        // If the server replied with an error, throw an exception.
        else if (response.getType() == IQ.Type.ERROR) {
            throw new XMPPException(response.getError());
        }
        return ((PrivateDataResult)response).getPrivateData();
    }

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

        // Setup a listener for the reply to the set operation.
        String packetID = privateDataSet.getPacketID();
        PacketCollector collector = con.createPacketCollector(new PacketIDFilter(packetID));

        // Send the private data.
        con.sendPacket(privateDataSet);

        // Wait up to five seconds for a response from the server.
        IQ response = (IQ)collector.nextResult(5000);
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
                    Object provider = getPrivateDataProvider(elementName, namespace);
                    // If there is a registered provider, use it.
                    if (provider != null) {
                        if (provider instanceof PrivateDataProvider) {
                            privateData = ((PrivateDataProvider)provider).parsePrivateData(parser);
                        }
                        // Otherwise it's a JavaBean, so use introspection.
                        else {
                            privateData = parseWithIntrospection(elementName, (Class)provider,
                                    parser);
                        }
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

    private static PrivateData parseWithIntrospection(String elementName,
            Class objectClass, XmlPullParser parser) throws Exception
    {
        boolean done = false;
        PrivateData object = (PrivateData)objectClass.newInstance();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String stringValue = parser.nextText();
                PropertyDescriptor descriptor = new PropertyDescriptor(name, objectClass);
                // Load the class type of the property.
                Class propertyType = descriptor.getPropertyType();
                // Get the value of the property by converting it from a
                // String to the correct object type.
                Object value = decode(propertyType, stringValue);
                // Set the value of the bean.
                descriptor.getWriteMethod().invoke(object, new Object[] { value });
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(elementName)) {
                    done = true;
                }
            }
        }
        return object;
    }

    /**
     * Decodes a String into an object of the specified type. If the object
     * type is not supported, null will be returned.
     *
     * @param type the type of the property.
     * @param value the encode String value to decode.
     * @return the String value decoded into the specified type.
     */
    private static Object decode(Class type, String value) throws Exception {
        if (type.getName().equals("java.lang.String")) {
            return value;
        }
        if (type.getName().equals("boolean")) {
            return Boolean.valueOf(value);
        }
        if (type.getName().equals("int")) {
            return Integer.valueOf(value);
        }
        if (type.getName().equals("long")) {
            return Long.valueOf(value);
        }
        if (type.getName().equals("float")) {
            return Float.valueOf(value);
        }
        if (type.getName().equals("double")) {
            return Double.valueOf(value);
        }
        if (type.getName().equals("java.lang.Class")) {
            return Class.forName(value);
        }
        return null;
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
