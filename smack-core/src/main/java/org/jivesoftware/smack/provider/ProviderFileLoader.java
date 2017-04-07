/**
 *
 * Copyright the original author or authors
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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;

/**
 * Loads the {@link IQProvider} and {@link ExtensionElementProvider} information from a standard provider file in preparation 
 * for loading into the {@link ProviderManager}.
 * 
 * @author Robin Collier
 *
 */
public class ProviderFileLoader implements ProviderLoader {
    private static final Logger LOGGER = Logger.getLogger(ProviderFileLoader.class.getName());

    private final Collection<IQProviderInfo> iqProviders = new LinkedList<IQProviderInfo>();
    private final Collection<ExtensionProviderInfo> extProviders  = new LinkedList<ExtensionProviderInfo>();
    private final Collection<StreamFeatureProviderInfo> sfProviders = new LinkedList<StreamFeatureProviderInfo>();

    private List<Exception> exceptions = new LinkedList<Exception>();

    public ProviderFileLoader(InputStream providerStream) {
        this(providerStream, ProviderFileLoader.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public ProviderFileLoader(InputStream providerStream, ClassLoader classLoader) {
        // Load processing providers.
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(providerStream, "UTF-8");
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    final String typeName = parser.getName();

                    try {
                        if (!"smackProviders".equals(typeName)) {
                            parser.next();
                            parser.next();
                            String elementName = parser.nextText();
                            parser.next();
                            parser.next();
                            String namespace = parser.nextText();
                            parser.next();
                            parser.next();
                            String className = parser.nextText();

                            try {
                                final Class<?> provider = classLoader.loadClass(className);
                                switch (typeName) {
                                case "iqProvider":
                                    // Attempt to load the provider class and then create
                                    // a new instance if it's an IQProvider. Otherwise, if it's
                                    // an IQ class, add the class object itself, then we'll use
                                    // reflection later to create instances of the class.
                                    // Add the provider to the map.
                                    if (IQProvider.class.isAssignableFrom(provider)) {
                                        IQProvider<IQ> iqProvider = (IQProvider<IQ>) provider.getConstructor().newInstance();
                                        iqProviders.add(new IQProviderInfo(elementName, namespace, iqProvider));
                                    }
                                    else {
                                        exceptions.add(new IllegalArgumentException(className + " is not a IQProvider"));
                                    }
                                    break;
                                case "extensionProvider":
                                    // Attempt to load the provider class and then create
                                    // a new instance if it's an ExtensionProvider. Otherwise, if it's
                                    // a PacketExtension, add the class object itself and
                                    // then we'll use reflection later to create instances
                                    // of the class.
                                    if (ExtensionElementProvider.class.isAssignableFrom(provider)) {
                                        ExtensionElementProvider<ExtensionElement> extensionElementProvider = (ExtensionElementProvider<ExtensionElement>) provider.getConstructor().newInstance();
                                        extProviders.add(new ExtensionProviderInfo(elementName, namespace,
                                                        extensionElementProvider));
                                    }
                                    else {
                                        exceptions.add(new IllegalArgumentException(className
                                                        + " is not a PacketExtensionProvider"));
                                    }
                                    break;
                                case "streamFeatureProvider":
                                    ExtensionElementProvider<ExtensionElement> streamFeatureProvider = (ExtensionElementProvider<ExtensionElement>) provider.getConstructor().newInstance();
                                    sfProviders.add(new StreamFeatureProviderInfo(elementName,
                                                    namespace,
                                                    streamFeatureProvider));
                                    break;
                                default:
                                    LOGGER.warning("Unknown provider type: " + typeName);
                                }
                            }
                            catch (ClassNotFoundException cnfe) {
                                LOGGER.log(Level.SEVERE, "Could not find provider class", cnfe);
                                exceptions.add(cnfe);
                            }
                            catch (InstantiationException ie) {
                                LOGGER.log(Level.SEVERE, "Could not instanciate " + className, ie);
                                exceptions.add(ie);
                            }
                        }
                    }
                    catch (IllegalArgumentException illExc) {
                        LOGGER.log(Level.SEVERE, "Invalid provider type found [" + typeName + "] when expecting iqProvider or extensionProvider", illExc);
                        exceptions.add(illExc);
                    }
                }
                eventType = parser.next();
            }
            while (eventType != XmlPullParser.END_DOCUMENT);
        }
        catch (Exception e){
            LOGGER.log(Level.SEVERE, "Unknown error occurred while parsing provider file", e);
            exceptions.add(e);
        }
        finally {
            try {
                providerStream.close();
            }
            catch (Exception e) {
                // Ignore.
            }
        }
    }

    @Override
    public Collection<IQProviderInfo> getIQProviderInfo() {
        return iqProviders;
    }

    @Override
    public Collection<ExtensionProviderInfo> getExtensionProviderInfo() {
        return extProviders;
    }

    @Override
    public Collection<StreamFeatureProviderInfo> getStreamFeatureProviderInfo() {
        return sfProviders;
    }

    public List<Exception> getLoadingExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
}
