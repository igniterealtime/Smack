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

package org.jivesoftware.smack.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmppElementUtil;

import org.jxmpp.util.XmppStringUtils;

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
 * mechanism is provided. IQ providers are registered programmatically or by creating a
 * providers file. The file is an XML
 * document that contains one or more iqProvider entries, as in the following example:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;smackProviders&gt;
 *     &lt;iqProvider&gt;
 *         &lt;elementName&gt;query&lt;/elementName&gt;
 *         &lt;namespace&gt;jabber:iq:time&lt;/namespace&gt;
 *         &lt;className&gt;org.jivesoftware.smack.packet.Time&lt;/className&gt;
 *     &lt;/iqProvider&gt;
 * &lt;/smackProviders&gt;</pre>
 *
 * Each IQ provider is associated with an element name and a namespace. If multiple provider
 * entries attempt to register to handle the same namespace, the first entry loaded from the
 * classpath will take precedence. The IQ provider class can either implement the IQProvider
 * interface, or extend the IQ class. In the former case, each IQProvider is responsible for
 * parsing the raw XML stream to create an IQ instance. In the latter case, bean introspection
 * is used to try to automatically set properties of the IQ instance using the values found
 * in the IQ stanza XML. For example, an XMPP time stanza resembles the following:
 * <pre>
 * &lt;iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'&gt;
 *     &lt;query xmlns='jabber:iq:time'&gt;
 *         &lt;utc&gt;20020910T17:58:35&lt;/utc&gt;
 *         &lt;tz&gt;MDT&lt;/tz&gt;
 *         &lt;display&gt;Tue Sep 10 12:58:35 2002&lt;/display&gt;
 *     &lt;/query&gt;
 * &lt;/iq&gt;</pre>
 *
 * In order for this stanza to be automatically mapped to the Time object listed in the
 * providers file above, it must have the methods setUtc(String), setTz(String), and
 * setDisplay(String). The introspection service will automatically try to convert the String
 * value from the XML into a boolean, int, long, float, double, or Class depending on the
 * type the IQ instance expects.<p>
 *
 * A pluggable system for stanza extensions, child elements in a custom namespace for
 * message and presence packets, also exists. Each extension provider
 * is registered with a name space in the smack.providers file as in the following example:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;smackProviders&gt;
 *     &lt;extensionProvider&gt;
 *         &lt;elementName&gt;x&lt;/elementName&gt;
 *         &lt;namespace&gt;jabber:iq:event&lt;/namespace&gt;
 *         &lt;className&gt;org.jivesoftware.smack.packet.MessageEvent&lt;/className&gt;
 *     &lt;/extensionProvider&gt;
 * &lt;/smackProviders&gt;</pre>
 *
 * If multiple provider entries attempt to register to handle the same element name and namespace,
 * the first entry loaded from the classpath will take precedence. Whenever a stanza extension
 * is found in a packet, parsing will be passed to the correct provider. Each provider
 * can either implement the PacketExtensionProvider interface or be a standard Java Bean. In
 * the former case, each extension provider is responsible for parsing the raw XML stream to
 * construct an object. In the latter case, bean introspection is used to try to automatically
 * set the properties of th class using the values in the stanza extension sub-element. When an
 * extension provider is not registered for an element name and namespace combination, Smack will
 * store all top-level elements of the sub-packet in DefaultPacketExtension object and then
 * attach it to the packet.<p>
 *
 * @author Matt Tucker
 */
public final class ProviderManager {

    private static final Map<String, ExtensionElementProvider<ExtensionElement>> extensionProviders = new ConcurrentHashMap<String, ExtensionElementProvider<ExtensionElement>>();
    private static final Map<String, IQProvider<IQ>> iqProviders = new ConcurrentHashMap<String, IQProvider<IQ>>();
    private static final Map<String, ExtensionElementProvider<ExtensionElement>> streamFeatureProviders = new ConcurrentHashMap<String, ExtensionElementProvider<ExtensionElement>>();
    private static final Map<String, NonzaProvider<? extends Nonza>> nonzaProviders = new ConcurrentHashMap<>();

    static {
        // Ensure that Smack is initialized by calling getVersion, so that user
        // registered providers do not get overwritten by a following Smack
        // initialization. This guarantees that Smack is initialized before a
        // new provider is registered
        SmackConfiguration.getVersion();
    }

    @SuppressWarnings("unchecked")
    public static void addLoader(ProviderLoader loader) {
        if (loader.getIQProviderInfo() != null) {
            for (IQProviderInfo info : loader.getIQProviderInfo()) {
                addIQProvider(info.getElementName(), info.getNamespace(), info.getProvider());
            }
        }

        if (loader.getExtensionProviderInfo() != null) {
            for (ExtensionProviderInfo info : loader.getExtensionProviderInfo()) {
                addExtensionProvider(info.getElementName(), info.getNamespace(), info.getProvider());
            }
        }

        if (loader.getStreamFeatureProviderInfo() != null) {
            for (StreamFeatureProviderInfo info : loader.getStreamFeatureProviderInfo()) {
                addStreamFeatureProvider(info.getElementName(), info.getNamespace(),
                                (ExtensionElementProvider<ExtensionElement>) info.getProvider());
            }
        }
    }

    /**
     * Returns the IQ provider registered to the specified XML element name and namespace.
     * For example, if a provider was registered to the element name "query" and the
     * namespace "jabber:iq:time", then the following stanza would trigger the provider:
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
    public static IQProvider<IQ> getIQProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        return iqProviders.get(key);
    }

    /**
     * Returns an unmodifiable collection of all IQProvider instances. Each object
     * in the collection will either be an IQProvider instance, or a Class object
     * that implements the IQProvider interface.
     *
     * @return all IQProvider instances.
     */
    public static List<IQProvider<IQ>> getIQProviders() {
        List<IQProvider<IQ>> providers = new ArrayList<>(iqProviders.size());
        providers.addAll(iqProviders.values());
        return providers;
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
    @SuppressWarnings("unchecked")
    public static void addIQProvider(String elementName, String namespace,
            Object provider) {
        validate(elementName, namespace);
        // First remove existing providers
        String key = removeIQProvider(elementName, namespace);
        if (provider instanceof IQProvider) {
            iqProviders.put(key, (IQProvider<IQ>) provider);
        } else {
            throw new IllegalArgumentException("Provider must be an IQProvider");
        }
    }

    /**
     * Removes an IQ provider with the specified element name and namespace. This
     * method is typically called to cleanup providers that are programmatically added
     * using the {@link #addIQProvider(String, String, Object) addIQProvider} method.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @return the key of the removed IQ Provider
     */
    public static String removeIQProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        iqProviders.remove(key);
        return key;
    }

    /**
     * Returns the stanza extension provider registered to the specified XML element name
     * and namespace. For example, if a provider was registered to the element name "x" and the
     * namespace "jabber:x:event", then the following stanza would trigger the provider:
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
     * @param elementName element name associated with extension provider.
     * @param namespace namespace associated with extension provider.
     * @return the extension provider.
     */
    public static ExtensionElementProvider<ExtensionElement> getExtensionProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
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
    @SuppressWarnings("unchecked")
    public static void addExtensionProvider(String elementName, String namespace,
            Object provider) {
        validate(elementName, namespace);
        // First remove existing providers
        String key = removeExtensionProvider(elementName, namespace);
        if (provider instanceof ExtensionElementProvider) {
            extensionProviders.put(key, (ExtensionElementProvider<ExtensionElement>) provider);
        } else {
            throw new IllegalArgumentException("Provider must be a PacketExtensionProvider");
        }
    }

    /**
     * Removes an extension provider with the specified element name and namespace. This
     * method is typically called to cleanup providers that are programmatically added
     * using the {@link #addExtensionProvider(String, String, Object) addExtensionProvider} method.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @return the key of the removed stanza extension provider
     */
    public static String removeExtensionProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        extensionProviders.remove(key);
        return key;
    }

    /**
     * Returns an unmodifiable collection of all PacketExtensionProvider instances. Each object
     * in the collection will either be a PacketExtensionProvider instance, or a Class object
     * that implements the PacketExtensionProvider interface.
     *
     * @return all PacketExtensionProvider instances.
     */
    public static List<ExtensionElementProvider<ExtensionElement>> getExtensionProviders() {
        List<ExtensionElementProvider<ExtensionElement>> providers = new ArrayList<>(extensionProviders.size());
        providers.addAll(extensionProviders.values());
        return providers;
    }

    public static ExtensionElementProvider<ExtensionElement> getStreamFeatureProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        return streamFeatureProviders.get(key);
    }

    public static void addStreamFeatureProvider(String elementName, String namespace, ExtensionElementProvider<ExtensionElement> provider) {
        validate(elementName, namespace);
        String key = getKey(elementName, namespace);
        streamFeatureProviders.put(key, provider);
    }

    public static void removeStreamFeatureProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        streamFeatureProviders.remove(key);
    }

    public static NonzaProvider<? extends Nonza> getNonzaProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        return getNonzaProvider(key);
    }

    public static NonzaProvider<? extends Nonza> getNonzaProvider(String key) {
        return nonzaProviders.get(key);
    }

    public static void addNonzaProvider(NonzaProvider<? extends Nonza> nonzaProvider) {
        Class<? extends Nonza> nonzaClass = nonzaProvider.getElementClass();
        String key = XmppElementUtil.getKeyFor(nonzaClass);
        nonzaProviders.put(key, nonzaProvider);
    }

    public static void removeNonzaProvider(Class<? extends Nonza> nonzaClass) {
        String key = XmppElementUtil.getKeyFor(nonzaClass);
        nonzaProviders.remove(key);
    }

    public static void removeNonzaProvider(String elementName, String namespace) {
        String key = getKey(elementName, namespace);
        nonzaProviders.remove(key);
    }

    private static String getKey(String elementName, String namespace) {
        return XmppStringUtils.generateKey(elementName, namespace);
    }

    private static void validate(String elementName, String namespace) {
        if (StringUtils.isNullOrEmpty(elementName)) {
            throw new IllegalArgumentException("elementName must not be null or empty");
        }
        if (StringUtils.isNullOrEmpty(namespace)) {
            throw new IllegalArgumentException("namespace must not be null or empty");
        }
    }
}
