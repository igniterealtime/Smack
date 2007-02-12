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

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "x";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "jabber:x:conference";

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
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
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