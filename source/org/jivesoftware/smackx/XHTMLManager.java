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

package org.jivesoftware.smackx;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.packet.*;

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
        XMPPConnection.addConnectionListener(new ConnectionEstablishedListener() {
            public void connectionEstablished(XMPPConnection connection) {
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
    public synchronized static void setServiceEnabled(XMPPConnection connection, boolean enabled) {
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
    public static boolean isServiceEnabled(XMPPConnection connection) {
        return ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(namespace);
    }

    /**
     * Returns true if the specified user handles XHTML messages.
     *
     * @param connection the connection to use to perform the service discovery
     * @param userID the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com
     * @return a boolean indicating whether the specified user handles XHTML messages
     */
    public static boolean isServiceEnabled(XMPPConnection connection, String userID) {
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
