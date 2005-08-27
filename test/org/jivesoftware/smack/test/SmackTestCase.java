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
package org.jivesoftware.smack.test;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import javax.net.SocketFactory;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.xmlpull.v1.*;
import org.xmlpull.mxp1.MXParser;

import junit.framework.TestCase;

/**
 * Base class for all the test cases which provides a pre-configured execution context. This 
 * means that any test case that subclassifies this base class will have access to a pool of 
 * connections and to the user of each connection. The maximum number of connections in the pool
 * can be controlled by the message {@link #getMaxConnections()} which every subclass must 
 * implement.<p>   
 * 
 * This base class defines a default execution context (i.e. host, port, chat domain and muc 
 * domain) which can be found in the file "config/test-case.xml". However, each subclass could
 * redefine the default configuration by providing its own configuration file (if desired). The
 * name of the configuration file must be of the form <test class name>.xml (e.g. RosterTest.xml).
 * The file must be placed in the folder "config". This folder is where the default configuration 
 * file is being held.
 *
 * @author Gaston Dombiak
 */
public abstract class SmackTestCase extends TestCase {

    private String host = "localhost";
    private String serviceName = "localhost";
    private int port = 5222;

    private String chatDomain = "chat.localhost";
    private String mucDomain = "conference.localhost";

    private XMPPConnection[] connections = null;

    /**
     * Constructor for SmackTestCase.
     * @param arg0
     */
    public SmackTestCase(String arg0) {
        super(arg0);
    }

    /**
     * Returns the maximum number of connections to initialize for this test case. All the 
     * initialized connections will be connected to the server using a new test account for 
     * each conection. 
     * 
     * @return the maximum number of connections to initialize for this test case.
     */
    protected abstract int getMaxConnections();

    /**
     * Returns a SocketFactory that will be used to create the socket to the XMPP server. By 
     * default no SocketFactory is used but subclasses my want to redefine this method.<p>
     * 
     * A custom SocketFactory allows fine-grained control of the actual connection to the XMPP 
     * server. A typical use for a custom SocketFactory is when connecting through a SOCKS proxy.
     * 
     * @return a SocketFactory that will be used to create the socket to the XMPP server.
     */
    protected SocketFactory getSocketFactory() {
        return null;
    }

    /**
     * Returns the XMPPConnection located at the requested position. Each test case holds a
     * pool of connections which is initialized while setting up the test case. The maximum
     * number of connections is controlled by the message {@link #getMaxConnections()} which
     * every subclass must implement.<p>   
     * 
     * If the requested position is greater than the connections size then an 
     * IllegalArgumentException will be thrown. 
     * 
     * @param index the position in the pool of the connection to look for.
     * @return the XMPPConnection located at the requested position.
     */
    protected XMPPConnection getConnection(int index) {
        if (index > getMaxConnections()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return connections[index];
    }

    /**
     * Returns the name of the user (e.g. johndoe) that is using the connection 
     * located at the requested position.
     * 
     * @param index the position in the pool of the connection to look for.
     * @return the user of the user (e.g. johndoe).
     */
    protected String getUsername(int index) {
        if (index > getMaxConnections()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        return "user" + index;
    }

    /**
     * Returns the bare XMPP address of the user (e.g. johndoe@jabber.org) that is
     * using the connection located at the requested position.
     * 
     * @param index the position in the pool of the connection to look for.
     * @return the bare XMPP address of the user (e.g. johndoe@jabber.org).
     */
    protected String getBareJID(int index) {
        return getUsername(index) + "@" + getConnection(index).getServiceName();
    }

    /**
     * Returns the full XMPP address of the user (e.g. johndoe@jabber.org/Smack) that is
     * using the connection located at the requested position.
     * 
     * @param index the position in the pool of the connection to look for.
     * @return the full XMPP address of the user (e.g. johndoe@jabber.org/Smack).
     */
    protected String getFullJID(int index) {
        return getBareJID(index) + "/Smack";
    }

    protected String getHost() {
        return host;
    }

    protected int getPort() {
        return port;
    }

    protected String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the default groupchat service domain.
     * 
     * @return the default groupchat service domain.
     */
    protected String getChatDomain() {
        return chatDomain;
    }

    /**
     * Returns the default MUC service domain.
     * 
     * @return the default MUC service domain.
     */
    protected String getMUCDomain() {
        return mucDomain;
    }

    protected void setUp() throws Exception {
        super.setUp();
        init();
        if (getMaxConnections() < 1) {
            return;
        }
        connections = new XMPPConnection[getMaxConnections()];
        try {
            // Connect to the server
            for (int i = 0; i < getMaxConnections(); i++) {
                if (getSocketFactory() == null) {
                    connections[i] = new XMPPConnection(host, port);
                }
                else {
                    connections[i] = new XMPPConnection(host, port, host, getSocketFactory());
                }
            }
            // Use the host name that the server reports. This is a good idea in most
            // cases, but could fail if the user set a hostname in their XMPP server
            // that will not resolve as a network connection.
            host = connections[0].getHost();
            serviceName = connections[0].getServiceName();
            // Create the test accounts
            if (!getConnection(0).getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");

            for (int i = 0; i < getMaxConnections(); i++) {
                // Create the test account
                try {
                    getConnection(i).getAccountManager().createAccount("user" + i, "user" + i);
                } catch (XMPPException e) {
                    // Do nothing if the accout already exists
                    if (e.getXMPPError().getCode() != 409) {
                        throw e;
                    }
                }
                // Login with the new test account
                getConnection(i).login("user" + i, "user" + i);
            }
            // Let the server process the available presences
            Thread.sleep(150);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        for (int i = 0; i < getMaxConnections(); i++) {
            // Delete the created account for the test
            getConnection(i).getAccountManager().deleteAccount();
            // Close the connection
            getConnection(i).close();

        }
    }

    /**
     * Initializes the context of the test case. We will first try to load the configuration from 
     * a file whose name is conformed by the test case class name plus an .xml extension 
     * (e.g RosterTest.xml). If no file was found under that name then we will try to load the 
     * default configuration for all the test cases from the file "config/test-case.xml".
     *
     */
    private void init() {
        try {
            boolean found = false;
            // Try to load the configutation from an XML file specific for this test case 
            Enumeration resources =
                ClassLoader.getSystemClassLoader().getResources(getConfigurationFilename());
            while (resources.hasMoreElements()) {
                found = parseURL((URL) resources.nextElement());
            }
            // If none was found then try to load the configuration from the default configuration 
            // file (i.e. "config/test-case.xml")
            if (!found) {
                resources = ClassLoader.getSystemClassLoader().getResources("config/test-case.xml");
                while (resources.hasMoreElements()) {
                    found = parseURL((URL) resources.nextElement());
                }
            }
            if (!found) {
                System.err.println("File config/test-case.xml not found. Using default config.");
            }
        }
        catch (Exception e) {
        }
    }

    /**
     * Returns true if the given URL was found and parsed without problems. The file provided
     * by the URL must contain information useful for the test case configuration, such us,
     * host and port of the server.  
     * 
     * @param url the url of the file to parse.
     * @return true if the given URL was found and parsed without problems.
     */
    private boolean parseURL(URL url) {
        boolean parsedOK = false;
        InputStream systemStream = null;
        try {
            systemStream = url.openStream();
            XmlPullParser parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(systemStream, "UTF-8");
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("host")) {
                        host = parser.nextText();
                    }
                    else if (parser.getName().equals("port")) {
                        port = parseIntProperty(parser, port);
                    }
                    else if (parser.getName().equals("serviceName")) {
                        serviceName = parser.nextText();
                    }
                    else if (parser.getName().equals("chat")) {
                        chatDomain = parser.nextText();
                    }
                    else if (parser.getName().equals("muc")) {
                        mucDomain = parser.nextText();
                    }
                }
                eventType = parser.next();
            }
            while (eventType != XmlPullParser.END_DOCUMENT);
            parsedOK = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                systemStream.close();
            }
            catch (Exception e) {
            }
        }
        return parsedOK;
    }

    private static int parseIntProperty(XmlPullParser parser, int defaultValue) throws Exception {
        try {
            return Integer.parseInt(parser.nextText());
        }
        catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return defaultValue;
        }
    }

    /**
     * Returns the name of the configuration file related to <b>this</b> test case. By default all 
     * the test cases will use the same configuration file. However, it's possible to override the
     * default configuration by providing a file of the form <test case class name>.xml
     * (e.g. RosterTest.xml).
     * 
     * @return the name of the configuration file related to this test case.
     */
    private String getConfigurationFilename() {
        String fullClassName = this.getClass().getName();
        int firstChar = fullClassName.lastIndexOf('.') + 1;
        return "config/" + fullClassName.substring(firstChar) + ".xml";
    }

}
