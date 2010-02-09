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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.XHTMLExtension;

import java.util.Iterator;

/**
 * Manages XHTML formatted texts within messages. A XHTMLManager provides a high level access to 
 * get and set XHTML bodies to messages, enable and disable XHTML support and check if remote XMPP
 * clients support XHTML.   
 * 
 * @author Gaston Dombiak
 */
public class XHTMLManager {

    private final static String namespace = "http://jabber.org/protocol/xhtml-im";

    // Enable the XHTML support on every established connection
    // The ServiceDiscoveryManager class should have been already initialized
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                XHTMLManager.setServiceEnabled(connection, true);
            }
        });
    }

    /**
     * Returns an Iterator for the XHTML bodies in the message. Returns null if 
     * the message does not contain an XHTML extension.
     *
     * @param message an XHTML message
     * @return an Iterator for the bodies in the message or null if none.
     */
    public static Iterator getBodies(Message message) {
        XHTMLExtension xhtmlExtension = (XHTMLExtension) message.getExtension("html", namespace);
        if (xhtmlExtension != null)
            return xhtmlExtension.getBodies();
        else
            return null;
    }

    /**
     * Adds an XHTML body to the message.
     *
     * @param message the message that will receive the XHTML body
     * @param body the string to add as an XHTML body to the message
     */
    public static void addBody(Message message, String body) {
        XHTMLExtension xhtmlExtension = (XHTMLExtension) message.getExtension("html", namespace);
        if (xhtmlExtension == null) {
            // Create an XHTMLExtension and add it to the message
            xhtmlExtension = new XHTMLExtension();
            message.addExtension(xhtmlExtension);
        }
        // Add the required bodies to the message
        xhtmlExtension.addBody(body);
    }

    /**
     * Returns true if the message contains an XHTML extension.
     *
     * @param message the message to check if contains an XHTML extentsion or not
     * @return a boolean indicating whether the message is an XHTML message
     */
    public static boolean isXHTMLMessage(Message message) {
        return message.getExtension("html", namespace) != null;
    }

    /**
     * Enables or disables the XHTML support on a given connection.<p>
     *  
     * Before starting to send XHTML messages to a user, check that the user can handle XHTML
     * messages. Enable the XHTML support to indicate that this client handles XHTML messages.  
     *
     * @param connection the connection where the service will be enabled or disabled
     * @param enabled indicates if the service will be enabled or disabled 
     */
    public synchronized static void setServiceEnabled(Connection connection, boolean enabled) {
        if (isServiceEnabled(connection) == enabled)
            return;

        if (enabled) {
            ServiceDiscoveryManager.getInstanceFor(connection).addFeature(namespace);
        }
        else {
            ServiceDiscoveryManager.getInstanceFor(connection).removeFeature(namespace);
        }
    }

    /**
     * Returns true if the XHTML support is enabled for the given connection.
     *
     * @param connection the connection to look for XHTML support
     * @return a boolean indicating if the XHTML support is enabled for the given connection
     */
    public static boolean isServiceEnabled(Connection connection) {
        return ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(namespace);
    }

    /**
     * Returns true if the specified user handles XHTML messages.
     *
     * @param connection the connection to use to perform the service discovery
     * @param userID the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com
     * @return a boolean indicating whether the specified user handles XHTML messages
     */
    public static boolean isServiceEnabled(Connection connection, String userID) {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(userID);
            return result.containsFeature(namespace);
        }
        catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }
}
