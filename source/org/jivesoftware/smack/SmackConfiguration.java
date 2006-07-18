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

package org.jivesoftware.smack;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

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

    private static final String SMACK_VERSION = "3.0.0";

    private static int packetReplyTimeout = 5000;
    private static int keepAliveInterval = 30000;

    private SmackConfiguration() {
    }

    /**
     * Loads the configuration from the smack-config.xml file.<p>
     * 
     * So far this means that:
     * 1) a set of classes will be loaded in order to execute their static init block
     * 2) retrieve and set the current Smack release
     */
    static {
        try {
            // Get an array of class loaders to try loading the providers files from.
            ClassLoader[] classLoaders = getClassLoaders();
            for (ClassLoader classLoader : classLoaders) {
                Enumeration configEnum = classLoader.getResources("META-INF/smack-config.xml");
                while (configEnum.hasMoreElements()) {
                    URL url = (URL) configEnum.nextElement();
                    InputStream systemStream = null;
                    try {
                        systemStream = url.openStream();
                        XmlPullParser parser = new MXParser();
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                        parser.setInput(systemStream, "UTF-8");
                        int eventType = parser.getEventType();
                        do {
                            if (eventType == XmlPullParser.START_TAG) {
                                if (parser.getName().equals("className")) {
                                    // Attempt to load the class so that the class can get initialized
                                    parseClassToLoad(parser);
                                }
                                else if (parser.getName().equals("packetReplyTimeout")) {
                                    packetReplyTimeout =
                                            parseIntProperty(parser, packetReplyTimeout);
                                }
                                else if (parser.getName().equals("keepAliveInterval")) {
                                    keepAliveInterval = parseIntProperty(parser, keepAliveInterval);
                                }
                            }
                            eventType = parser.next();
                        }
                        while (eventType != XmlPullParser.END_DOCUMENT);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            systemStream.close();
                        }
                        catch (Exception e) {
                            // Ignore.
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        keepAliveInterval = interval;
    }

    private static void parseClassToLoad(XmlPullParser parser) throws Exception {
        String className = parser.nextText();
        // Attempt to load the class so that the class can get initialized
        try {
            Class.forName(className);
        }
        catch (ClassNotFoundException cnfe) {
            System.err.println("Error! A startup class specified in smack-config.xml could " +
                    "not be loaded: " + className);
        }
    }

    private static int parseIntProperty(XmlPullParser parser, int defaultValue)
            throws Exception
    {
        try {
            return Integer.parseInt(parser.nextText());
        }
        catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * Returns an array of class loaders to load resources from.
     *
     * @return an array of ClassLoader instances.
     */
    private static ClassLoader[] getClassLoaders() {
        ClassLoader[] classLoaders = new ClassLoader[2];
        classLoaders[0] = SmackConfiguration.class.getClassLoader();
        classLoaders[1] = Thread.currentThread().getContextClassLoader();
        return classLoaders;
    }
}
