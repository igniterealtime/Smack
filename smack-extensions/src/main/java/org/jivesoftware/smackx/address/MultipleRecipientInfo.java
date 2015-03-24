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

package org.jivesoftware.smackx.address;

import org.jivesoftware.smackx.address.packet.MultipleAddresses;
import org.jxmpp.jid.Jid;

import java.util.List;

/**
 * MultipleRecipientInfo keeps information about the multiple recipients extension included
 * in a received packet. Among the information we can find the list of TO and CC addresses.
 *
 * @author Gaston Dombiak
 */
public class MultipleRecipientInfo {

    MultipleAddresses extension;

    MultipleRecipientInfo(MultipleAddresses extension) {
        this.extension = extension;
    }

    /**
     * Returns the list of {@link org.jivesoftware.smackx.address.packet.MultipleAddresses.Address}
     * that were the primary recipients of the packet.
     *
     * @return list of primary recipients of the packet.
     */
    public List<MultipleAddresses.Address> getTOAddresses() {
        return extension.getAddressesOfType(MultipleAddresses.Type.to);
    }

    /**
     * Returns the list of {@link org.jivesoftware.smackx.address.packet.MultipleAddresses.Address}
     * that were the secondary recipients of the packet.
     *
     * @return list of secondary recipients of the packet.
     */
    public List<MultipleAddresses.Address> getCCAddresses() {
        return extension.getAddressesOfType(MultipleAddresses.Type.cc);
    }

    /**
     * Returns the JID of a MUC room to which responses should be sent or <tt>null</tt>  if
     * no specific address was provided. When no specific address was provided then the reply
     * can be sent to any or all recipients. Otherwise, the user should join the specified room
     * and send the reply to the room.
     *
     * @return the JID of a MUC room to which responses should be sent or <tt>null</tt>  if
     *         no specific address was provided.
     */
    // TODO should return BareJid
    public Jid getReplyRoom() {
        List<MultipleAddresses.Address> replyRoom = extension.getAddressesOfType(MultipleAddresses.Type.replyroom);
        return replyRoom.isEmpty() ? null : replyRoom.get(0).getJid();
    }

    /**
     * Returns true if the received packet should not be replied. Use
     * {@link MultipleRecipientManager#reply(org.jivesoftware.smack.XMPPConnection, org.jivesoftware.smack.packet.Message, org.jivesoftware.smack.packet.Message)}
     * to send replies. 
     *
     * @return true if the received packet should not be replied.
     */
    public boolean shouldNotReply() {
        return !extension.getAddressesOfType(MultipleAddresses.Type.noreply).isEmpty();
    }

    /**
     * Returns the address to which all replies are requested to be sent or <tt>null</tt> if
     * no specific address was provided. When no specific address was provided then the reply
     * can be sent to any or all recipients.
     *
     * @return the address to which all replies are requested to be sent or <tt>null</tt> if
     *         no specific address was provided.
     */
    public MultipleAddresses.Address getReplyAddress() {
        List<MultipleAddresses.Address> replyTo = extension.getAddressesOfType(MultipleAddresses.Type.replyto);
        return replyTo.isEmpty() ? null : replyTo.get(0);
    }
}
