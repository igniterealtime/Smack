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
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

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
    private final String password;
    private final String reason;
    private final boolean _continue;
    private final String thread;

    /**
     * Creates a new group chat invitation to the specified room address.
     * GroupChat room addresses are in the form <code>room@service</code>,
     * where <code>service</code> is the name of group chat server, such as
     * <code>chat.example.com</code>.
     *
     * @param roomAddress the address of the group chat room.
     * @deprecated use {@link #GroupChatInvitation(EntityBareJid)} instead.
     */
    @Deprecated
    public GroupChatInvitation(String roomAddress) {
        this(JidCreate.entityBareFromOrThrowUnchecked(roomAddress));
    }

    public GroupChatInvitation(EntityBareJid roomAddress) {
        this(roomAddress, null, null, false, null);
    }

    public GroupChatInvitation(EntityBareJid mucJid, String password, String reason, boolean _continue, String thread) {
        this.roomAddress = Objects.requireNonNull(mucJid);
        this.password = password;
        this.reason = reason;
        this._continue = _continue;
        this.thread = thread;
    }

    /**
     * Returns the address of the group chat room. GroupChat room addresses
     * are in the form <code>room@service</code>, where <code>service</code> is
     * the name of group chat server, such as <code>chat.example.com</code>.
     *
     * TODO: Remove in Smack 4.5
     * @deprecated use {@link #getRoomAddressJid()} instead.
     * @return the address of the group chat room.
     */
    @Deprecated
    public String getRoomAddress() {
        return roomAddress.asEntityBareJidString();
    }

    /**
     * Returns the address of the group chat room as an {@link EntityBareJid}.
     *
     * @return room address
     */
    public EntityBareJid getRoomAddressJid() {
        return roomAddress;
    }

    /**
     * Returns the password which is used to join the room.
     * This value can be null if no password is required.
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Return the reason of invitation.
     *
     * @return reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns true if the invitation represents the continuation of a one-to-one chat.
     * The chat continues the thread returned by {@link #getThread()}.
     *
     * @return true if this is a continued one-to-one chat, false otherwise.
     */
    public boolean isContinue() {
        return _continue;
    }

    /**
     * In case of a continuation, this returns the thread name of the one-to-one chat that is being continued.
     *
     * @return thread
     */
    public String getThread() {
        return thread;
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
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .optBooleanAttribute(ATTR_CONTINUE, isContinue())
                .attribute(ATTR_JID, getRoomAddressJid())
                .optAttribute(ATTR_PASSWORD, getPassword())
                .optAttribute(ATTR_REASON, getReason())
                .optAttribute(ATTR_THREAD, getThread())
                .closeEmptyElement();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) -> equalsBuilder
                        .append(getRoomAddressJid(), other.getRoomAddressJid())
                        .append(getPassword(), other.getPassword())
                        .append(getReason(), other.getReason())
        .append(isContinue(), other.isContinue())
        .append(getThread(), other.getThread()));
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getRoomAddressJid())
                .append(getPassword())
                .append(getReason())
                .append(isContinue())
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
