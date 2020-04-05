/**
 *
 * Copyright 2003-2007 Jive Software, 2014-2020 Florian Schmaus
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

package org.jivesoftware.smack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.compress.provider.CompressedProvider;
import org.jivesoftware.smack.compress.provider.FailureProvider;
import org.jivesoftware.smack.compression.CompressionModuleDescriptor;
import org.jivesoftware.smack.compression.Java7ZlibInputOutputStream;
import org.jivesoftware.smack.compression.XmppCompressionManager;
import org.jivesoftware.smack.compression.zlib.ZlibXmppCompressionFactory;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.isr.InstantStreamResumptionModuleDescriptor;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smack.provider.BindIQProvider;
import org.jivesoftware.smack.provider.BodyElementProvider;
import org.jivesoftware.smack.provider.MessageSubjectElementProvider;
import org.jivesoftware.smack.provider.MessageThreadElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.provider.SaslChallengeProvider;
import org.jivesoftware.smack.provider.SaslFailureProvider;
import org.jivesoftware.smack.provider.SaslSuccessProvider;
import org.jivesoftware.smack.provider.TlsFailureProvider;
import org.jivesoftware.smack.provider.TlsProceedProvider;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.sasl.core.SASLXOauth2Mechanism;
import org.jivesoftware.smack.sasl.core.SCRAMSHA1Mechanism;
import org.jivesoftware.smack.sasl.core.ScramSha1PlusMechanism;
import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;


public final class SmackInitialization {
    static final String SMACK_VERSION;

    private static final String DEFAULT_CONFIG_FILE = "org.jivesoftware.smack/smack-config.xml";

    private static final Logger LOGGER = Logger.getLogger(SmackInitialization.class.getName());

    /**
     * Loads the configuration from the smack-config.xml and system properties file.
     * <p>
     * So far this means that:
     * 1) a set of classes will be loaded in order to execute their static init block
     * 2) retrieve and set the current Smack release
     * 3) set DEBUG
     */
    static {
        String smackVersion;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(FileUtils.getStreamForClasspathFile("org.jivesoftware.smack/version", null), StandardCharsets.UTF_8));
            smackVersion = reader.readLine();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not determine Smack version", e);
            smackVersion = "unknown";
        } finally {
            CloseableUtil.maybeClose(reader, LOGGER);
        }
        SMACK_VERSION = smackVersion;

        String disabledClasses = System.getProperty("smack.disabledClasses");
        if (disabledClasses != null) {
            String[] splitDisabledClasses = disabledClasses.split(",");
            for (String s : splitDisabledClasses) SmackConfiguration.disabledSmackClasses.add(s);
        }

        InputStream configFileStream;
        try {
            configFileStream = FileUtils.getStreamForClasspathFile(DEFAULT_CONFIG_FILE, null);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not load Smack configuration file", e);
        }

        try {
            processConfigFile(configFileStream, null);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not parse Smack configuration file", e);
        }

        // Add the Java7 compression handler first, since it's preferred
        SmackConfiguration.compressionHandlers.add(new Java7ZlibInputOutputStream());

        XmppCompressionManager.registerXmppCompressionFactory(ZlibXmppCompressionFactory.INSTANCE);

        // Use try block since we may not have permission to get a system
        // property (for example, when an applet).
        try {
            // Only overwrite DEBUG if it is set via the 'smack.debugEnabled' property. To prevent DEBUG_ENABLED
            // = true, which could be set e.g. via a static block from user code, from being overwritten by the property not set
            if (Boolean.getBoolean("smack.debugEnabled")) {
                SmackConfiguration.DEBUG = true;
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not handle debugEnable property on Smack initialization", e);
        }

        SASLAuthentication.registerSASLMechanism(new SCRAMSHA1Mechanism());
        SASLAuthentication.registerSASLMechanism(new ScramSha1PlusMechanism());
        SASLAuthentication.registerSASLMechanism(new SASLXOauth2Mechanism());
        SASLAuthentication.registerSASLMechanism(new SASLAnonymous());

        ProviderManager.addIQProvider(Bind.ELEMENT, Bind.NAMESPACE, new BindIQProvider());
        ProviderManager.addExtensionProvider(Body.ELEMENT, Body.NAMESPACE, new BodyElementProvider());
        ProviderManager.addExtensionProvider(Message.Thread.ELEMENT, Message.Thread.NAMESPACE, new MessageThreadElementProvider());
        ProviderManager.addExtensionProvider(Message.Subject.ELEMENT, Message.Subject.NAMESPACE, new MessageSubjectElementProvider());

        ProviderManager.addNonzaProvider(SaslChallengeProvider.INSTANCE);
        ProviderManager.addNonzaProvider(SaslSuccessProvider.INSTANCE);
        ProviderManager.addNonzaProvider(SaslFailureProvider.INSTANCE);
        ProviderManager.addNonzaProvider(TlsProceedProvider.INSTANCE);
        ProviderManager.addNonzaProvider(TlsFailureProvider.INSTANCE);
        ProviderManager.addNonzaProvider(CompressedProvider.INSTANCE);
        ProviderManager.addNonzaProvider(FailureProvider.INSTANCE);

        SmackConfiguration.addModule(Bind2ModuleDescriptor.class);
        SmackConfiguration.addModule(CompressionModuleDescriptor.class);
        SmackConfiguration.addModule(InstantStreamResumptionModuleDescriptor.class);

        SmackConfiguration.smackInitialized = true;
    }

    public static void processConfigFile(InputStream cfgFileStream,
                    Collection<Exception> exceptions) throws Exception {
        processConfigFile(cfgFileStream, exceptions, SmackInitialization.class.getClassLoader());
    }

    public static void processConfigFile(InputStream cfgFileStream,
                    Collection<Exception> exceptions, ClassLoader classLoader) throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(cfgFileStream);
        XmlPullParser.Event eventType = parser.getEventType();
        do {
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("startupClasses")) {
                    parseClassesToLoad(parser, false, exceptions, classLoader);
                }
                else if (parser.getName().equals("optionalStartupClasses")) {
                    parseClassesToLoad(parser, true, exceptions, classLoader);
                }
            }
            eventType = parser.next();
        }
        while (eventType != XmlPullParser.Event.END_DOCUMENT);
        CloseableUtil.maybeClose(cfgFileStream, LOGGER);
    }

    private static void parseClassesToLoad(XmlPullParser parser, boolean optional,
                    Collection<Exception> exceptions, ClassLoader classLoader)
                    throws Exception {
        final String startName = parser.getName();
        XmlPullParser.Event eventType;
        outerloop: do {
            eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT && "className".equals(parser.getName())) {
                String classToLoad = parser.nextText();
                if (SmackConfiguration.isDisabledSmackClass(classToLoad)) {
                    continue outerloop;
                }

                try {
                    loadSmackClass(classToLoad, optional, classLoader);
                } catch (Exception e) {
                    // Don't throw the exception if an exceptions collection is given, instead
                    // record it there. This is used for unit testing purposes.
                    if (exceptions != null) {
                        exceptions.add(e);
                    } else {
                        throw e;
                    }
                }
            }
        }
        while (!(eventType == XmlPullParser.Event.END_ELEMENT && startName.equals(parser.getName())));
    }

    private static void loadSmackClass(String className, boolean optional, ClassLoader classLoader) throws Exception {
        Class<?> initClass;
        try {
            // Attempt to load and initialize the class so that all static initializer blocks of
            // class are executed
            initClass = Class.forName(className, true, classLoader);
        }
        catch (ClassNotFoundException cnfe) {
            Level logLevel;
            if (optional) {
                logLevel = Level.FINE;
            }
            else {
                logLevel = Level.WARNING;
            }
            LOGGER.log(logLevel, "A startup class '" + className + "' could not be loaded.");
            if (!optional) {
                throw cnfe;
            } else {
                return;
            }
        }
        if (SmackInitializer.class.isAssignableFrom(initClass)) {
            SmackInitializer initializer = (SmackInitializer) initClass.getConstructor().newInstance();
            List<Exception> exceptions = initializer.initialize();
            if (exceptions == null || exceptions.size() == 0) {
                LOGGER.log(Level.FINE, "Loaded SmackInitializer " + className);
            } else {
                for (Exception e : exceptions) {
                    LOGGER.log(Level.SEVERE, "Exception in loadSmackClass", e);
                }
            }
        } else {
            LOGGER.log(Level.FINE, "Loaded " + className);
        }
    }
}
