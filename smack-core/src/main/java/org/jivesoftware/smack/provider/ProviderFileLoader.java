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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;

/**
 * Loads the {@link IQProvider} and {@link PacketExtensionProvider} information from a standard provider file in preparation 
 * for loading into the {@link ProviderManager}.
 * 
 * @author Robin Collier
 *
 */
public class ProviderFileLoader implements ProviderLoader {
    private static final Logger LOGGER = Logger.getLogger(ProviderFileLoader.class.getName());

    private Collection<IQProviderInfo> iqProviders;
    private Collection<ExtensionProviderInfo> extProviders;

    private List<Exception> exceptions = new LinkedList<Exception>();

    public ProviderFileLoader(InputStream providerStream) {
        this(providerStream, ProviderFileLoader.class.getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public ProviderFileLoader(InputStream providerStream, ClassLoader classLoader) {
        iqProviders = new ArrayList<IQProviderInfo>();
        extProviders = new ArrayList<ExtensionProviderInfo>();
        
        // Load processing providers.
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(providerStream, "UTF-8");
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    String typeName = parser.getName();
                    
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
                                // Attempt to load the provider class and then create
                                // a new instance if it's an IQProvider. Otherwise, if it's
                                // an IQ class, add the class object itself, then we'll use
                                // reflection later to create instances of the class.
                                if ("iqProvider".equals(typeName)) {
                                    // Add the provider to the map.
                                    
                                    if (IQProvider.class.isAssignableFrom(provider)) {
                                        iqProviders.add(new IQProviderInfo(elementName, namespace, (IQProvider) provider.newInstance()));
                                    }
                                    else if (IQ.class.isAssignableFrom(provider)) {
                                        iqProviders.add(new IQProviderInfo(elementName, namespace, (Class<? extends IQ>)provider));
                                    }
                                }
                                else {
                                    // Attempt to load the provider class and then create
                                    // a new instance if it's an ExtensionProvider. Otherwise, if it's
                                    // a PacketExtension, add the class object itself and
                                    // then we'll use reflection later to create instances
                                    // of the class.
                                    if (PacketExtensionProvider.class.isAssignableFrom(provider)) {
                                        extProviders.add(new ExtensionProviderInfo(elementName, namespace, (PacketExtensionProvider) provider.newInstance()));
                                    }
                                    else if (PacketExtension.class.isAssignableFrom(provider)) {
                                        extProviders.add(new ExtensionProviderInfo(elementName, namespace, provider));
                                    }
                                }
                            }
                            catch (ClassNotFoundException cnfe) {
                                LOGGER.log(Level.SEVERE, "Could not find provider class", cnfe);
                                exceptions.add(cnfe);
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

    public List<Exception> getLoadingExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
}
