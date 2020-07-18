/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.muc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;

import org.jxmpp.jid.EntityBareJid;

/**
 * Smacks API for XEP-0249: Direct MUC Invitations.
 * Use this instead of {@link org.jivesoftware.smackx.muc.packet.MUCUser.Invite}.
 *
 * To invite a user to a group chat, use {@link #inviteToMuc(MultiUserChat, EntityBareJid)}.
 *
 * In order to listen for incoming invitations, register a {@link DirectMucInvitationListener} using
 * {@link #addInvitationListener(DirectMucInvitationListener)}.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0249.html">Direct MUC Invitations</a>
 */
public final class DirectMucInvitationManager extends Manager {

    private static final Map<XMPPConnection, DirectMucInvitationManager> INSTANCES = new WeakHashMap<>();
    private final List<DirectMucInvitationListener> directMucInvitationListeners = new ArrayList<>();
    private final ServiceDiscoveryManager serviceDiscoveryManager;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(DirectMucInvitationManager::getInstanceFor);
    }

    public static synchronized DirectMucInvitationManager getInstanceFor(XMPPConnection connection) {
        DirectMucInvitationManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new DirectMucInvitationManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private DirectMucInvitationManager(XMPPConnection connection) {
        super(connection);
        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        connection().addAsyncStanzaListener(stanza -> {
            GroupChatInvitation invitation = stanza.getExtension(GroupChatInvitation.class);
            for (DirectMucInvitationListener listener : directMucInvitationListeners) {
                listener.invitationReceived(invitation, stanza);
            }
        }, new StanzaExtensionFilter(GroupChatInvitation.ELEMENT, GroupChatInvitation.NAMESPACE));
        serviceDiscoveryManager.addFeature(GroupChatInvitation.NAMESPACE);
    }

    public void inviteToMuc(MultiUserChat muc, EntityBareJid user)
            throws SmackException.NotConnectedException, InterruptedException {
        inviteToMuc(muc, user, null, null, false, null);
    }

    public void inviteToMuc(MultiUserChat muc, EntityBareJid user, String password, String reason, boolean continueAsOneToOneChat, String thread)
            throws SmackException.NotConnectedException, InterruptedException {
        inviteToMuc(user, new GroupChatInvitation(muc.getRoom(), reason, password, continueAsOneToOneChat, thread));
    }

    public void inviteToMuc(EntityBareJid jid, GroupChatInvitation invitation) throws SmackException.NotConnectedException, InterruptedException {
        Message invitationMessage = MessageBuilder.buildMessage()
                .to(jid)
                .addExtension(invitation)
                .build();
        connection().sendStanza(invitationMessage);
    }

    public boolean userSupportsInvitations(EntityBareJid jid)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return serviceDiscoveryManager.supportsFeature(jid, GroupChatInvitation.NAMESPACE);
    }

    public synchronized void addInvitationListener(DirectMucInvitationListener listener) {
        directMucInvitationListeners.add(listener);
    }

    public synchronized void removeInvitationListener(DirectMucInvitationListener listener) {
        directMucInvitationListeners.remove(listener);
    }
}
