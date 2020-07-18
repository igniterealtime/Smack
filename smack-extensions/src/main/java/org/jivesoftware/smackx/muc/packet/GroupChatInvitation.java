/**
 *
 * Copyright 2003-2007 Jive Software, 2020 Paul Schaub.
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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.EntityBareJid;

/**
 * A group chat invitation stanza extension, which is used to invite other
 * users to a group chat room.
 *
 * This implementation now conforms to XEP-0249: Direct MUC Invitations,
 * while staying backwards compatible to legacy MUC invitations.
 *
 * @author Matt Tucker
 * @author Paul Schaub
 */
public class GroupChatInvitation implements ExtensionElement {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT = "x";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "jabber:x:conference";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public static final String ATTR_CONTINUE = "continue";
    public static final String ATTR_JID = "jid";
    public static final String ATTR_PASSWORD = "password";
    public static final String ATTR_REASON = "reason";
    public static final String ATTR_THREAD = "thread";

    private final EntityBareJid roomAddress;
    private final String reason;
    private final String password;
    private final String thread;
    private final boolean continueAsOneToOneChat;

    /**
     * Creates a new group chat invitation to the specified room address.
     * GroupChat room addresses are in the form <code>room@service</code>,
     * where <code>service</code> is the name of group chat server, such as
     * <code>chat.example.com</code>.
     *
     * @param roomAddress the address of the group chat room.
     */
    public GroupChatInvitation(EntityBareJid roomAddress) {
        this(roomAddress, null, null, false, null);
    }

    /**
     * Creates a new group chat invitation to the specified room address.
     * GroupChat room addresses are in the form <code>room@service</code>,
     * where <code>service</code> is the name of group chat server, such as
     * <code>chat.example.com</code>.
     *
     * @param roomAddress the address of the group chat room.
     * @param reason the purpose for the invitation
     * @param password specifies a password needed for entry
     * @param continueAsOneToOneChat specifies if the groupchat room continues a one-to-one chat having the designated thread
     * @param thread the thread to continue
     */
    public GroupChatInvitation(EntityBareJid roomAddress,
                               String reason,
                               String password,
                               boolean continueAsOneToOneChat,
                               String thread) {
        this.roomAddress = Objects.requireNonNull(roomAddress);
        this.reason = reason;
        this.password = password;
        this.continueAsOneToOneChat = continueAsOneToOneChat;
        this.thread = thread;
    }

    /**
     * Returns the purpose for the invitation.
     *
     * @return the address of the group chat room.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the password needed for entry.
     *
     * @return the password needed for entry
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the thread to continue.
     *
     * @return the thread to continue.
     */
    public String getThread() {
        return thread;
    }

    /**
     * Returns whether the groupchat room continues a one-to-one chat.
     *
     * @return whether the groupchat room continues a one-to-one chat.
     */
    public boolean continueAsOneToOneChat() {
        return continueAsOneToOneChat;
    }

    /**
     * Returns the address of the group chat room. GroupChat room addresses
     * are in the form <code>room@service</code>, where <code>service</code> is
     * the name of group chat server, such as <code>chat.example.com</code>.
     *
     * @return the address of the group chat room.
     */
    public EntityBareJid getRoomAddress() {
        return roomAddress;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.jidAttribute(getRoomAddress());
        xml.optAttribute(ATTR_REASON, getReason());
        xml.optAttribute(ATTR_PASSWORD, getPassword());
        xml.optAttribute(ATTR_THREAD, getThread());
        xml.optBooleanAttribute(ATTR_CONTINUE, continueAsOneToOneChat());

        xml.closeEmptyElement();
        return xml;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) -> equalsBuilder
                        .append(getRoomAddress(), other.getRoomAddress())
                        .append(getPassword(), other.getPassword())
                        .append(getReason(), other.getReason())
        .append(continueAsOneToOneChat(), other.continueAsOneToOneChat())
        .append(getThread(), other.getThread()));
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getRoomAddress())
                .append(getPassword())
                .append(getReason())
                .append(continueAsOneToOneChat())
                .append(getThread())
                .build();
    }

    /**
     * Get the group chat invitation from the given stanza.
     * @param packet TODO javadoc me please
     * @return the GroupChatInvitation or null
     */
    public static GroupChatInvitation from(Stanza packet) {
        return packet.getExtension(GroupChatInvitation.class);
    }

}
