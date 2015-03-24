/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.muc.packet;

import java.io.IOException;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
 * con.sendStanza(message);
 * </pre>
 *
 * To listen for group chat invitations, use a StanzaExtensionFilter for the
 * <tt>x</tt> element name and <tt>jabber:x:conference</tt> namespace, as in the
 * following code example:
 *
 * <pre>
 * PacketFilter filter = new StanzaExtensionFilter("x", "jabber:x:conference");
 * // Create a packet collector or packet listeners using the filter...
 * </pre>
 *
 * <b>Note</b>: this protocol is outdated now that the Multi-User Chat (MUC) XEP is available
 * (<a href="http://www.xmpp.org/extensions/jep-0045.html">XEP-45</a>). However, most
 * existing clients still use this older protocol. Once MUC support becomes more
 * widespread, this API may be deprecated.
 * 
 * @author Matt Tucker
 */
public class GroupChatInvitation implements ExtensionElement {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT = "x";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "jabber:x:conference";

    private final String roomAddress;

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
        return ELEMENT;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("jid", getRoomAddress());
        xml.closeEmptyElement();
        return xml;
    }

    /**
     * 
     * @param packet
     * @return the GroupChatInvitation or null
     * @deprecated use {@link #from(Stanza)} instead
     */
    @Deprecated
    public static GroupChatInvitation getFrom(Stanza packet) {
        return from(packet);
    }

    /**
     * 
     * @param packet
     * @return the GroupChatInvitation or null
     */
    public static GroupChatInvitation from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }

    public static class Provider extends ExtensionElementProvider<GroupChatInvitation> {

        @Override
        public GroupChatInvitation parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException {
            String roomAddress = parser.getAttributeValue("", "jid");
            // Advance to end of extension.
            parser.next();
            return new GroupChatInvitation(roomAddress);
        }
    }
}
