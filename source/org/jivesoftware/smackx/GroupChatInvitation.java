/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2003 Jive Software. All rights reserved.
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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A group chat invitation packet extension, which is used to invite other
 * users to a group chat room. To invite a user to a group chat room, address
 * a new message to the user and set the room name appropriately, as in the
 * following code example:
 *
 * <pre>
 * Message message = new Message("user@chat.example.com");
 * message.setBody("Join me for a group chat!");
 * message.addExtension(new GroupChatInvitation("room@chat.example.com"););
 * con.sendPacket(message);
 * </pre>
 *
 * To listen for group chat invitations, use a PacketExtensionFilter for the
 * <tt>x</tt> element name and <tt>jabber:x:conference</tt> namespace, as in the
 * following code example:
 *
 * <pre>
 * PacketFilter filter = new PacketExtensionFilter("x", "jabber:x:conference");
 * // Create a packet collector or packet listeners using the filter...
 * </pre>
 *
 * <b>Note</b>: this protocol is outdated now that the Multi-User Chat (MUC) JEP is available
 * (<a href="http://www.jabber.org/jeps/jep-0045.html">JEP-45</a>). However, most
 * existing clients still use this older protocol. Once MUC support becomes more
 * widespread, this API may be deprecated.
 * 
 * @author Matt Tucker
 */
public class GroupChatInvitation implements PacketExtension {

    private String roomAddress;

    /**
     * Creates a new group chat invitation to the specified room address.
     * GroupChat room addresses are in the form <tt>room@service</tt>,
     * where <tt>service</tt> is the name of groupchat server, such as
     * <tt>chat.example.com</tt>.
     *
     * @param roomAddress the address of the group chat room.
     */
    public GroupChatInvitation(String roomAddress) {
        this.roomAddress = roomAddress;
    }

    /**
     * Returns the address of the group chat room. GroupChat room addresses
     * are in the form <tt>room@service</tt>, where <tt>service</tt> is
     * the name of groupchat server, such as <tt>chat.example.com</tt>.
     *
     * @return the address of the group chat room.
     */
    public String getRoomAddress() {
        return roomAddress;
    }

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "jabber:x:conference";
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<x xmlns=\"jabber:x:conference\" jid=\"").append(roomAddress).append("\"/>");
        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {
        public PacketExtension parseExtension (XmlPullParser parser) throws Exception {
            String roomAddress = parser.getAttributeValue("", "jid");
            // Advance to end of extension.
            parser.next();
            return new GroupChatInvitation(roomAddress);
        }
    }
}