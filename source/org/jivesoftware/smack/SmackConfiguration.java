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

package org.jivesoftware.smack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.parsing.ExceptionThrowingCallback;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.util.FileUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

/**
 * Represents the configuration of Smack. The configuration is used for:
 * <ul>
 *      <li> Initializing classes by loading them at start-up.
 *      <li> Getting the current Smack version.
 *      <li> Getting and setting global library behavior, such as the period of time
 *          to wait for replies to packets from the server. Note: setting these values
 *          via the API will override settings in the configuration file.
 * </ul>
 *
 * Configuration settings are stored in META-INF/smack-config.xml (typically inside the
 * smack.jar file).
 * 
 * @author Gaston Dombiak
 */
public final class SmackConfiguration {
    private static final String SMACK_VERSION = "3.4.0";
    private static final String DEFAULT_CONFIG_FILE = "classpath:META-INF/smack-config.xml"; 
    
    private static final Logger log = Logger.getLogger(SmackConfiguration.class.getName());
    
    private static InputStream configFileStream;
    
    private static int packetReplyTimeout = 5000;
    private static int keepAliveInterval = 30000;
    private static List<String> defaultMechs = new ArrayList<String>();

    private static boolean localSocks5ProxyEnabled = true;
    private static int localSocks5ProxyPort = 7777;
    private static int packetCollectorSize = 5000;
    
    private static boolean initialized = false;

    /**
     * The default parsing exception callback is {@link ExceptionThrowingCallback} which will
     * throw an exception and therefore disconnect the active connection.
     */
    private static ParsingExceptionCallback defaultCallback = new ExceptionThrowingCallback();

    /**
     * This automatically enables EntityCaps for new connections if it is set to true
     */
    private static boolean autoEnableEntityCaps = true;

    private SmackConfiguration() {
    }

    /**
     * Loads the configuration from the smack-config.xml file.<p>
     * 
     * So far this means that:
     * 1) a set of classes will be loaded in order to execute their static init block
     * 2) retrieve and set the current Smack release
     */
    
    /**
     * Sets the location of the config file on the classpath. Only required if changing from the default location of <i>classpath:META-INF/smack-config.xml</i>.
     * 
     * <p>
     * This method must be called before accessing any other class in Smack.
     * 
     * @param configFileUrl The location of the config file.
     * @param loader The classloader to use if the URL has a protocol of <b>classpath</> and the file is not located on the default classpath.  
     * This can be set to null to use defaults and is ignored for all other protocols.
     * @throws IllegalArgumentException If the config URL is invalid in that it cannot open an {@link InputStream}
     */
    public static void setConfigFileUrl(String configFileUrl, ClassLoader loader) {
        try {
            configFileStream = FileUtils.getStreamForUrl(configFileUrl, loader);
        } 
        catch (Exception e) {
            throw new IllegalArgumentException("Failed to create input stream from specified file URL ["+ configFileUrl + "]", e);
        } 
        initialize();
    }
    
    /**
     * Sets the {@link InputStream} representing the smack configuration file. This can be used to override the default with something that is not on the classpath.
     * <p>
     * This method must be called before accessing any other class in Smack.
     * @param configFile
     */
    public static void setConfigFileStream(InputStream configFile) {
        configFileStream = configFile;
        initialize();
    }
    
    /**
     * Returns the Smack version information, eg "1.3.0".
     * 
     * @return the Smack version information.
     */
    public static String getVersion() {
        return SMACK_VERSION;
    }

    /**
     * Returns the number of milliseconds to wait for a response from
     * the server. The default value is 5000 ms.
     * 
     * @return the milliseconds to wait for a response from the server
     */
    public static int getPacketReplyTimeout() {
        initialize();
        
        // The timeout value must be greater than 0 otherwise we will answer the default value
        if (packetReplyTimeout <= 0) {
            packetReplyTimeout = 5000;
        }
        return packetReplyTimeout;
    }

    /**
     * Sets the number of milliseconds to wait for a response from
     * the server.
     * 
     * @param timeout the milliseconds to wait for a response from the server
     */
    public static void setPacketReplyTimeout(int timeout) {
        initialize();

        if (timeout <= 0) {
            throw new IllegalArgumentException();
        }
        packetReplyTimeout = timeout;
    }

    /**
     * Returns the number of milleseconds delay between sending keep-alive
     * requests to the server. The default value is 30000 ms. A value of -1
     * mean no keep-alive requests will be sent to the server.
     *
     * @return the milliseconds to wait between keep-alive requests, or -1 if
     *      no keep-alive should be sent.
     */
    public static int getKeepAliveInterval() {
        initialize();
        return keepAliveInterval;
    }

    /**
     * Sets the number of milleseconds delay between sending keep-alive
     * requests to the server. The default value is 30000 ms. A value of -1
     * mean no keep-alive requests will be sent to the server.
     *
     * @param interval the milliseconds to wait between keep-alive requests,
     *      or -1 if no keep-alive should be sent.
     */
    public static void setKeepAliveInterval(int interval) {
        initialize();
        keepAliveInterval = interval;
    }

    /**
     * Gets the default max size of a packet collector before it will delete 
     * the older packets.
     * 
     * @return The number of packets to queue before deleting older packets.
     */
    public static int getPacketCollectorSize() {
        initialize();
    	return packetCollectorSize;
    }

    /**
     * Sets the default max size of a packet collector before it will delete 
     * the older packets.
     * 
     * @param The number of packets to queue before deleting older packets.
     */
    public static void setPacketCollectorSize(int collectorSize) {
        initialize();
    	packetCollectorSize = collectorSize;
    }
    
    /**
     * Add a SASL mechanism to the list to be used.
     *
     * @param mech the SASL mechanism to be added
     */
    public static void addSaslMech(String mech) {
        initialize();

        if(! defaultMechs.contains(mech) ) {
            defaultMechs.add(mech);
        }
    }

   /**
     * Add a Collection of SASL mechanisms to the list to be used.
     *
     * @param mechs the Collection of SASL mechanisms to be added
     */
    public static void addSaslMechs(Collection<String> mechs) {
        initialize();

        for(String mech : mechs) {
            addSaslMech(mech);
        }
    }

    /**
     * Remove a SASL mechanism from the list to be used.
     *
     * @param mech the SASL mechanism to be removed
     */
    public static void removeSaslMech(String mech) {
        initialize();
        defaultMechs.remove(mech);
    }

   /**
     * Remove a Collection of SASL mechanisms to the list to be used.
     *
     * @param mechs the Collection of SASL mechanisms to be removed
     */
    public static void removeSaslMechs(Collection<String> mechs) {
        initialize();
        defaultMechs.removeAll(mechs);
    }

    /**
     * Returns the list of SASL mechanisms to be used. If a SASL mechanism is
     * listed here it does not guarantee it will be used. The server may not
     * support it, or it may not be implemented.
     *
     * @return the list of SASL mechanisms to be used.
     */
    public static List<String> getSaslMechs() {
        return Collections.unmodifiableList(defaultMechs);
    }

    /**
     * Returns true if the local Socks5 proxy should be started. Default is true.
     * 
     * @return if the local Socks5 proxy should be started
     */
    public static boolean isLocalSocks5ProxyEnabled() {
        initialize();
        return localSocks5ProxyEnabled;
    }

    /**
     * Sets if the local Socks5 proxy should be started. Default is true.
     * 
     * @param localSocks5ProxyEnabled if the local Socks5 proxy should be started
     */
    public static void setLocalSocks5ProxyEnabled(boolean localSocks5ProxyEnabled) {
        initialize();
        SmackConfiguration.localSocks5ProxyEnabled = localSocks5ProxyEnabled;
    }

    /**
     * Return the port of the local Socks5 proxy. Default is 7777.
     * 
     * @return the port of the local Socks5 proxy
     */
    public static int getLocalSocks5ProxyPort() {
        initialize();
        return localSocks5ProxyPort;
    }

    /**
     * Sets the port of the local Socks5 proxy. Default is 7777. If you set the port to a negative
     * value Smack tries the absolute value and all following until it finds an open port.
     * 
     * @param localSocks5ProxyPort the port of the local Socks5 proxy to set
     */
    public static void setLocalSocks5ProxyPort(int localSocks5ProxyPort) {
        initialize();
        SmackConfiguration.localSocks5ProxyPort = localSocks5ProxyPort;
    }

    /**
     * Check if Entity Caps are enabled as default for every new connection
     * @return
     */
    public static boolean autoEnableEntityCaps() {
        initialize();
        return autoEnableEntityCaps;
    }

    /**
     * Set if Entity Caps are enabled or disabled for every new connection
     * 
     * @param true if Entity Caps should be auto enabled, false if not
     */
    public static void setAutoEnableEntityCaps(boolean b) {
        initialize();
        autoEnableEntityCaps = b;
    }

    /**
     * Set the default parsing exception callback for all newly created connections
     *
     * @param callback
     * @see ParsingExceptionCallback
     */
    public static void setDefaultParsingExceptionCallback(ParsingExceptionCallback callback) {
        initialize();
        defaultCallback = callback;
    }

    /**
     * Returns the default parsing exception callback
     * 
     * @return the default parsing exception callback
     * @see ParsingExceptionCallback
     */
    public static ParsingExceptionCallback getDefaultParsingExceptionCallback() {
        initialize();
        return defaultCallback;
    }

    private static void parseClassToLoad(XmlPullParser parser) throws Exception {
        String className = parser.nextText();
        // Attempt to load the class so that the class can get initialized
        try {
            Class<?> initClass = Class.forName(className);
            
            if (SmackInitializer.class.isAssignableFrom(initClass)) {
                SmackInitializer initializer = (SmackInitializer) initClass.newInstance();
                initializer.initialize();
            }
        }
        catch (ClassNotFoundException cnfe) {
            log.log(Level.WARNING, "A startup class [" + className + "] specified in smack-config.xml could not be loaded: ");
        }
    }

    private static int parseIntProperty(XmlPullParser parser, int defaultValue)
            throws Exception
    {
        try {
            return Integer.parseInt(parser.nextText());
        }
        catch (NumberFormatException nfe) {
            log.log(Level.SEVERE, "Could not parse integer", nfe);
            return defaultValue;
        }
    }

    /*
     * Order of precedence for config file is VM arg, setConfigXXX methods and embedded default file location.
     */
    private static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        String configFileLocation = System.getProperty("smack.config.file");

        if (configFileLocation != null) {
            try {
                configFileStream = FileUtils.getStreamForUrl(configFileLocation, null);
            }
            catch (Exception e) {
                log.log(Level.SEVERE, "Error creating input stream for config file [" + configFileLocation + "] from VM argument", e);
            }
        }
            
        if (configFileStream == null) {
            try {
                configFileStream = FileUtils.getStreamForUrl(DEFAULT_CONFIG_FILE, null);
            }
            catch (Exception e) {
                log.log(Level.INFO, "Could not create input stream for default config file [" + DEFAULT_CONFIG_FILE + "]", e);
            }
        }
        
        if (configFileStream != null) {
            readFile(configFileStream);
        }
        else {
            log.log(Level.INFO, "No configuration file found");
        }
    }

    private static void readFile(InputStream cfgFileStream) {
        XmlPullParser parser = new MXParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(cfgFileStream, "UTF-8");
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("className")) {
                        // Attempt to load the class so that the class can get initialized
                        parseClassToLoad(parser);
                    }
                    else if (parser.getName().equals("packetReplyTimeout")) {
                        packetReplyTimeout = parseIntProperty(parser, packetReplyTimeout);
                    }
                    else if (parser.getName().equals("keepAliveInterval")) {
                        keepAliveInterval = parseIntProperty(parser, keepAliveInterval);
                    }
                    else if (parser.getName().equals("mechName")) {
                        defaultMechs.add(parser.nextText());
                    }
                    else if (parser.getName().equals("localSocks5ProxyEnabled")) {
                        localSocks5ProxyEnabled = Boolean.parseBoolean(parser.nextText());
                    }
                    else if (parser.getName().equals("localSocks5ProxyPort")) {
                        localSocks5ProxyPort = parseIntProperty(parser, localSocks5ProxyPort);
                    }
                    else if (parser.getName().equals("packetCollectorSize")) {
                        packetCollectorSize = parseIntProperty(parser, packetCollectorSize);
                    }
                    else if (parser.getName().equals("autoEnableEntityCaps")) {
                        autoEnableEntityCaps = Boolean.parseBoolean(parser.nextText());
                    }
                }
                eventType = parser.next();
            } while (eventType != XmlPullParser.END_DOCUMENT);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error occurred while reading config file", e);
        }
        finally {
            try {
                cfgFileStream.close();
            } catch (IOException e) {
                log.log(Level.INFO, "Error while closing config file input stream", e);
            }
        }
    }
}
