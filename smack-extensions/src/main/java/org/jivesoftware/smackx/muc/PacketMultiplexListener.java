/**
 *
 * Copyright 2003-2006 Jive Software.
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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.MessageWithSubjectFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.packet.MUCUser;

/**
 * The single <code>PacketListener</code> used by each {@link MultiUserChat}
 * for all basic processing of presence, and message packets targeted to that chat.
 *
 * @author Larry Kirschner
 */
class PacketMultiplexListener implements PacketListener {

    private static final PacketFilter PRESENCE_FILTER = new PacketTypeFilter(Presence.class);
    private static final PacketFilter DECLINES_FILTER = new PacketExtensionFilter(MUCUser.ELEMENT,
                    MUCUser.NAMESPACE);

    private ConnectionDetachedPacketCollector<Message> messageCollector;
    private PacketListener presenceListener;
    private PacketListener subjectListener;
    private PacketListener declinesListener;

    public PacketMultiplexListener(
            ConnectionDetachedPacketCollector<Message> messageCollector,
            PacketListener presenceListener,
            PacketListener subjectListener, PacketListener declinesListener) {
        if (messageCollector == null) {
            throw new IllegalArgumentException("MessageCollector is null");
        }
        if (presenceListener == null) {
            throw new IllegalArgumentException("Presence listener is null");
        }
        if (subjectListener == null) {
            throw new IllegalArgumentException("Subject listener is null");
        }
        if (declinesListener == null) {
            throw new IllegalArgumentException("Declines listener is null");
        }
        this.messageCollector = messageCollector;
        this.presenceListener = presenceListener;
        this.subjectListener = subjectListener;
        this.declinesListener = declinesListener;
    }

    public void processPacket(Packet p) throws NotConnectedException {
        if (PRESENCE_FILTER.accept(p)) {
            presenceListener.processPacket(p);
        }
        else if (MessageTypeFilter.GROUPCHAT.accept(p)) {
            messageCollector.processPacket((Message) p);

            if (MessageWithSubjectFilter.INSTANCE.accept(p)) {
                subjectListener.processPacket(p);
            }
        }
        else if (DECLINES_FILTER.accept(p)) {
            declinesListener.processPacket(p);
        }
    }

}
