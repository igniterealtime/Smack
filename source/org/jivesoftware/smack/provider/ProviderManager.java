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
 * By default, Smack only knows how to process IQ packets with query sub-packets that
 * are in a few namespaces:<ul>
 *      <li>jabber:iq:auth
 *      <li>jabber:iq:roster
 *      <li>jabber:iq:register</ul>
 *
 * Because many more IQ types are part of XMPP and its extensions, a pluggable IQ parsing
 * mechanism is provided. IQ providers are registered by creating a smack.providers file
 * in the WEB-INF directory of your JAR file. The file is an XML document that contains
 * one or more iqProvider entries, as in the following example:
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
 * Each IQ provider is associated with an element name and a namespace. If multiple provider entries attempt to
 * register to handle the same namespace, the first entry loaded from the classpath will
 * take precedence. The IQ provider class can either implement the IQProvider interface,
 * or extend the IQ class. In the former case, each IQProvider is responsible for parsing
 * the raw XML stream to create an IQ instance. In the latter case, bean introspection is
 * used to try to automatically set properties of the IQ instance using the values found
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
 * setDisplay(tz). The introspection service will automatically try to convert the String
 * value from the XML into a boolean, int, long, float, double, or Class depending on the
 * type the IQ instance expects.<p>
 *
 * A pluggable system for the &lt;x&gt; portion of packets also exists. Each x provider
 * is registered with a name space in the smack.providers file as in the following example:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;smackProviders&gt;
 *     &lt;xProvider namespace="jabber:iq:event"
 *                   className="org.jivesoftware.smack.packet.MessageEvent"/&gt;
 * &lt;/smackProviders&gt;</pre>
 *
 * If multiple provider entries attempt to register to handle the same namespace, the
 * first entry loaded from the classpath will take precedence. Whenever an &lt;x&gt; element
 * is found in a packet, parsing will be passed to the correct provider. Each provider
 * can either implement the XProvider or be a standard Java Bean. In the former case,
 * each XProvider is responsible for parsing the raw XML stream to contruct an object.
 * In the latter case, bean introspection is used to try to automatically set the
 * properties of the class using the values in the x sub-element. When a XProvider is
 * not registered for a namespace, Smack will store all top-level elements of the sub-packet
 * in a Map and attach it to the packet.
 *
 * @author Matt Tucker
 */
public class ProviderManager {

    private static Map extensionProviders = new Hashtable();
    private static Map iqProviders = new Hashtable();

    static {
        // Load IQ processing providers.
        try {
            Enumeration enum = ProviderManager.class.getClassLoader().getResources(
                    "WEB-INF/smack.providers");
            while (enum.hasMoreElements()) {
                URL url = (URL)enum.nextElement();
                java.io.InputStream providerStream = null;
                try {
                    providerStream = url.openStream();
                    XmlPullParser parser = getParserInstance();
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
                                        if (PacketExtensionProvider.class.isAssignableFrom(provider)) {
                                            extensionProviders.put(key, provider.newInstance());
                                        }
                                        else if (PacketExtension.class.isAssignableFrom(provider)) {
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
        catch (Exception e) { }
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
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @return
     */
    public synchronized static Object getIQProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return iqProviders.get(key);
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
     * @param elementName
     * @param namespace
     * @return
     */
    public synchronized static Object getExtensionProvider(String elementName, String namespace) {
        String key = getProviderKey(elementName, namespace);
        return extensionProviders.get(key);
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
     * Returns an XML parser instance.
     *
     * @return an XML parser instance.
     */
    private static XmlPullParser getParserInstance() {
        XmlPullParser parser = null;
        try {
            final String defaultProviderName = "org.xmlpull.mxp1.MXParserFactory";
            XmlPullParserFactory factory = null;
            try {
                // Attempt to load a factory implementation using a system property
                // and a classloader context.
                factory = XmlPullParserFactory.newInstance(
                        System.getProperty(XmlPullParserFactory.PROPERTY_NAME),
                        Thread.currentThread().getContextClassLoader().getClass());
            }
            catch (Exception e) {
                if (factory == null) {
                    // Loading failed. Therefore, use the hardcoded default.
                    factory = XmlPullParserFactory.newInstance(defaultProviderName, null);
                }
            }
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
        return parser;
    }
}