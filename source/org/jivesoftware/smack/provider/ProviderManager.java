/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.xmlpull.v1.*;

import java.util.*;
import java.net.URL;

/**
 * Manages providers for parsing custom XML sub-documents of XMPP packets. Two types of
 * providers exist:<ul>
 *      <li>IQProvider -- parses IQ requests into Java objects.
 *      <li>PacketExtension -- parses XML sub-documents attached to packets into
 *          PacketExtension instances.</ul>
 *
 * <b>IQProvider</b><p>
 *
 * By default, Smack only knows how to process IQ packets with sub-packets that
 * are in a few namespaces such as:<ul>
 *      <li>jabber:iq:auth
 *      <li>jabber:iq:roster
 *      <li>jabber:iq:register</ul>
 *
 * Because many more IQ types are part of XMPP and its extensions, a pluggable IQ parsing
 * mechanism is provided. IQ providers are registered programatically or by creating a
 * smack.providers file in the META-INF directory of your JAR file. The file is an XML
 * document that contains one or more iqProvider entries, as in the following example:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;smackProviders&gt;
 *     &lt;iqProvider&gt;
 *         &lt;elementName&gt;query&lt;/elementName&gt;
 *         &lt;namespace&gt;jabber:iq:time&lt;/namespace&gt;
 *         &lt;className&gt;org.jivesoftware.smack.packet.Time&lt/className&gt;
 *     &lt;/iqProvider&gt;
 * &lt;/smackProviders&gt;</pre>
 *
 * Each IQ provider is associated with an element name and a namespace. If multiple provider
 * entries attempt to register to handle the same namespace, the first entry loaded from the
 * classpath will take precedence. The IQ provider class can either implement the IQProvider
 * interface, or extend the IQ class. In the former case, each IQProvider is responsible for
 * parsing the raw XML stream to create an IQ instance. In the latter case, bean introspection
 * is used to try to automatically set properties of the IQ instance using the values found
 * in the IQ packet XML. For example, an XMPP time packet resembles the following:
 * <pre>
 * &lt;iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'&gt;
 *     &lt;query xmlns='jabber:iq:time'&gt;
 *         &lt;utc&gt;20020910T17:58:35&lt;/utc&gt;
 *         &lt;tz&gt;MDT&lt;/tz&gt;
 *         &lt;display&gt;Tue Sep 10 12:58:35 2002&lt;/display&gt;
 *     &lt;/query&gt;
 * &lt;/iq&gt;</pre>
 *
 * In order for this packet to be automatically mapped to the Time object listed in the
 * providers file above, it must have the methods setUtc(String), setTz(String), and
 * setDisplay(String). The introspection service will automatically try to convert the String
 * value from the XML into a boolean, int, long, float, double, or Class depending on the
 * type the IQ instance expects.<p>
 *
 * A pluggable system for packet extensions, child elements in a custom namespace for
 * message and presence packets, also exists. Each extension provider
 * is registered with a name space in the smack.providers file as in the following example:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;smackProviders&gt;
 *     &lt;extensionProvider&gt;
 *         &lt;elementName&gt;x&lt;/elementName&gt;
 *         &lt;namespace&gt;jabber:iq:event&lt;/namespace&gt;
 *         &lt;className&gt;org.jivesoftware.smack.packet.MessageEvent&lt/className&gt;
 *     &lt;/extensionProvider&gt;
 * &lt;/smackProviders&gt;</pre>
 *
 * If multiple provider entries attempt to register to handle the same element name and namespace,
 * the first entry loaded from the classpath will take precedence. Whenever a packet extension
 * is found in a packet, parsing will be passed to the correct provider. Each provider
 * can either implement the PacketExtensionProvider interface or be a standard Java Bean. In
 * the former case, each extension provider is responsible for parsing the raw XML stream to
 * contruct an object. In the latter case, bean introspection is used to try to automatically
 * set the properties of the class using the values in the packet extension sub-element. When an
 * extension provider is not registered for an element name and namespace combination, Smack will
 * store all top-level elements of the sub-packet in DefaultPacketExtension object and then
 * attach it to the packet.
 *
 * @author Matt Tucker
 */
public class ProviderManager {

    private static Map extensionProviders = new Hashtable();
    private static Map iqProviders = new Hashtable();

    static {
        // Load IQ processing providers.
        try {
            // Get an array of class loaders to try loading the providers files from.
            ClassLoader[] classLoaders = getClassLoaders();
            for (int i=0; i<classLoaders.length; i++) {
                Enumeration providerEnum = classLoaders[i].getResources(
                        "META-INF/smack.providers");
                while (providerEnum.hasMoreElements()) {
                    URL url = (URL)providerEnum.nextElement();
                    java.io.InputStream providerStream = null;
                    try {
                        providerStream = url.openStream();
                        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
                                "org.xmlpull.mxp1.MXParserFactory", null);
                        factory.setNamespaceAware(true);
                        XmlPullParser parser = factory.newPullParser();
                        parser.setInput(providerStream, "UTF-8");
                        int eventType = parser.getEventType();
                        do {
                            if (eventType == XmlPullParser.START_TAG) {
                                if (parser.getName().equals("iqProvider")) {
                                    parser.next();
                                    parser.next();
                                    String elementName = parser.nextText();
                                    parser.next();
                                    parser.next();
                                    String namespace = parser.nextText();
                                    parser.next();
                                    parser.next();
                                    String className = parser.nextText();
                                    // Only add the provider for the namespace if one isn't
                                    // already registered.
                                    String key = getProviderKey(elementName, namespace);
                                    if (!iqProviders.containsKey(key)) {
                                        // Attempt to load the provider class and then create
                                        // a new instance if it's an IQProvider. Otherwise, if it's
                                        // an IQ class, add the class object itself, then we'll use
                                        // reflection later to create instances of the class.
                                        try {
                                            // Add the provider to the map.
                                            Class provider = Class.forName(className);
                                            if (IQProvider.class.isAssignableFrom(provider)) {
                                                iqProviders.put(key, provider.newInstance());
                                            }
                                            else if (IQ.class.isAssignableFrom(provider)) {
                                                iqProviders.put(key, provider);
                                            }
                                        }
                                        catch (ClassNotFoundException cnfe) {
                                            cnfe.printStackTrace();
                                        }
                                    }
                                }
                                else if (parser.getName().equals("extensionProvider")) {
                                    parser.next();
                                    parser.next();
                                    String elementName = parser.nextText();
                                    parser.next();
                                    parser.next();
                                    String namespace = parser.nextText();
                                    parser.next();
                                    parser.next();
                                    String className = parser.nextText();
                                    // Only add the provider for the namespace if one isn't
                                    // already registered.
                                    String key = getProviderKey(elementName, namespace);
                                    if (!extensionProviders.containsKey(key)) {
                                        // Attempt to load the provider class and then create
                                        // a new instance if it's a Provider. Otherwise, if it's
                                        // a PacketExtension, add the class object itself and
                                        // then we'll use reflection later to create instances
                                        // of the class.
                                        try {
                                            // Add the provider to the map.
                                            Class provider = Class.forName(className);
                                            if (PacketExtensionProvider.class.isAssignableFrom(
                                                    provider))
                                            {
                                                extensionProviders.put(key, provider.newInstance());
                                            }
                                            else if (PacketExtension.class.isAssignableFrom(
                                                    provider))
                                            {
                                                extensionProviders.put(key, provider);
                                            }
                                        }
                                        catch (ClassNotFoundException cnfe) {
                                            cnfe.printStackTrace();
                                        }
                                    }
                                }
                            }
                            eventType = parser.next();
                        } while (eventType != XmlPullParser.END_DOCUMENT);
                    }
                    finally {
                        try { providerStream.close(); }
                        catch (Exception e) { }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the IQ provider registered to the specified XML element name and namespace.
     * For example, if a provider was registered to the element name "query" and the
     * namespace "jabber:iq:time", then the following packet would trigger the provider:
     *
     * <pre>
     * &lt;iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'&gt;
     *     &lt;query xmlns='jabber:iq:time'&gt;
     *         &lt;utc&gt;20020910T17:58:35&lt;/utc&gt;
     *         &lt;tz&gt;MDT&lt;/tz&gt;
     *         &lt;display&gt;Tue Sep 10 12:58:35 2002&lt;/display&gt;
     *     &lt;/query&gt;
     * &lt;/iq&gt;</pre>
     *
     * <p>Note: this method is generally only called by the internal Smack classes.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @return the IQ provider.
     */
    public static Object getIQProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return iqProviders.get(key);
    }

    /**
     * Returns an Iterator for all IQProvider instances.
     *
     * @return an Iterator for all IQProvider instances.
     */
    public static Iterator getIQProviders() {
        return Collections.unmodifiableCollection(new HashMap(iqProviders).values()).iterator();
    }

    /**
     * Adds an IQ provider (must be an instance of IQProvider or Class object that is an IQ)
     * with the specified element name and name space. The provider will override any providers
     * loaded through the classpath.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the IQ provider.
     */
    public static void addIQProvider(String elementName, String namespace,
            Object provider)
    {
        if (!(provider instanceof IQProvider || (provider instanceof Class &&
                IQ.class.isAssignableFrom((Class)provider))))
        {
            throw new IllegalArgumentException("Provider must be an IQProvider " +
                    "or a Class instance.");
        }
        String key = getProviderKey(elementName, namespace);
        iqProviders.put(key, provider);
    }

    /**
     * Returns the packet extension provider registered to the specified XML element name
     * and namespace. For example, if a provider was registered to the element name "x" and the
     * namespace "jabber:x:event", then the following packet would trigger the provider:
     *
     * <pre>
     * &lt;message to='romeo@montague.net' id='message_1'&gt;
     *     &lt;body&gt;Art thou not Romeo, and a Montague?&lt;/body&gt;
     *     &lt;x xmlns='jabber:x:event'&gt;
     *         &lt;composing/&gt;
     *     &lt;/x&gt;
     * &lt;/message&gt;</pre>
     *
     * <p>Note: this method is generally only called by the internal Smack classes.
     *
     * @param elementName
     * @param namespace
     * @return the extenion provider.
     */
    public static Object getExtensionProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return extensionProviders.get(key);
    }

    /**
     * Adds an extension provider with the specified element name and name space. The provider
     * will override any providers loaded through the classpath. The provider must be either
     * a PacketExtensionProvider instance, or a Class object of a Javabean.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the extension provider.
     */
    public static void addExtensionProvider(String elementName, String namespace,
            Object provider)
    {
        if (!(provider instanceof PacketExtensionProvider || provider instanceof Class)) {
            throw new IllegalArgumentException("Provider must be a PacketExtensionProvider " +
                    "or a Class instance.");
        }
        String key = getProviderKey(elementName, namespace);
        extensionProviders.put(key, provider);
    }

    /**
     * Returns an Iterator for all PacketExtensionProvider instances.
     *
     * @return an Iterator for all PacketExtensionProvider instances.
     */
    public static Iterator getExtensionProviders() {
        return Collections.unmodifiableCollection(
                new HashMap(extensionProviders).values()).iterator();
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
     * Returns an array of class loaders to load resources from.
     *
     * @return an array of ClassLoader instances.
     */
    private static ClassLoader[] getClassLoaders() {
        ClassLoader[] classLoaders = new ClassLoader[2];
        classLoaders[0] = new ProviderManager().getClass().getClassLoader();
        classLoaders[1] = Thread.currentThread().getContextClassLoader();
        return classLoaders;
    }

    private ProviderManager() {

    }
}