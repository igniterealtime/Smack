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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.FeatureNotSupportedException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.address.packet.MultipleAddresses;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A MultipleRecipientManager allows to send packets to multiple recipients by making use of
 * <a href="http://www.xmpp.org/extensions/jep-0033.html">XEP-33: Extended Stanza Addressing</a>.
 * It also allows to send replies to packets that were sent to multiple recipients.
 *
 * @author Gaston Dombiak
 */
public class MultipleRecipientManager {

    /**
     * Sends the specified stanza(/packet) to the collection of specified recipients using the
     * specified connection. If the server has support for XEP-33 then only one
     * stanza(/packet) is going to be sent to the server with the multiple recipient instructions.
     * However, if XEP-33 is not supported by the server then the client is going to send
     * the stanza(/packet) to each recipient.
     *
     * @param connection the connection to use to send the packet.
     * @param packet     the stanza(/packet) to send to the list of recipients.
     * @param to         the collection of JIDs to include in the TO list or <tt>null</tt> if no TO
     *                   list exists.
     * @param cc         the collection of JIDs to include in the CC list or <tt>null</tt> if no CC
     *                   list exists.
     * @param bcc        the collection of JIDs to include in the BCC list or <tt>null</tt> if no BCC
     *                   list exists.
     * @throws FeatureNotSupportedException if special XEP-33 features where requested, but the
     *         server does not support them.
     * @throws XMPPErrorException if server does not support XEP-33: Extended Stanza Addressing and
     *                       some XEP-33 specific features were requested.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public static void send(XMPPConnection connection, Stanza packet, Collection<String> to, Collection<String> cc, Collection<String> bcc) throws NoResponseException, XMPPErrorException, FeatureNotSupportedException, NotConnectedException
   {
        send(connection, packet, to, cc, bcc, null, null, false);
    }

    /**
     * Sends the specified stanza(/packet) to the collection of specified recipients using the specified
     * connection. If the server has support for XEP-33 then only one stanza(/packet) is going to be sent to
     * the server with the multiple recipient instructions. However, if XEP-33 is not supported by
     * the server then the client is going to send the stanza(/packet) to each recipient.
     * 
     * @param connection the connection to use to send the packet.
     * @param packet the stanza(/packet) to send to the list of recipients.
     * @param to the collection of JIDs to include in the TO list or <tt>null</tt> if no TO list exists.
     * @param cc the collection of JIDs to include in the CC list or <tt>null</tt> if no CC list exists.
     * @param bcc the collection of JIDs to include in the BCC list or <tt>null</tt> if no BCC list
     *        exists.
     * @param replyTo address to which all replies are requested to be sent or <tt>null</tt>
     *        indicating that they can reply to any address.
     * @param replyRoom JID of a MUC room to which responses should be sent or <tt>null</tt>
     *        indicating that they can reply to any address.
     * @param noReply true means that receivers should not reply to the message.
     * @throws XMPPErrorException if server does not support XEP-33: Extended Stanza Addressing and
     *         some XEP-33 specific features were requested.
     * @throws NoResponseException if there was no response from the server.
     * @throws FeatureNotSupportedException if special XEP-33 features where requested, but the
     *         server does not support them.
     * @throws NotConnectedException 
     */
    public static void send(XMPPConnection connection, Stanza packet, Collection<String> to, Collection<String> cc, Collection<String> bcc,
            String replyTo, String replyRoom, boolean noReply) throws NoResponseException, XMPPErrorException, FeatureNotSupportedException, NotConnectedException {
        // Check if *only* 'to' is set and contains just *one* entry, in this case extended stanzas addressing is not
        // required at all and we can send it just as normal stanza without needing to add the extension element
        if (to != null && to.size() == 1 && (cc == null || cc.isEmpty()) && (bcc == null || bcc.isEmpty()) && !noReply
                        && StringUtils.isNullOrEmpty(replyTo) && StringUtils.isNullOrEmpty(replyRoom)) {
            String toJid = to.iterator().next();
            packet.setTo(toJid);
            connection.sendStanza(packet);
            return;
        }
        String serviceAddress = getMultipleRecipienServiceAddress(connection);
        if (serviceAddress != null) {
            // Send packet to target users using multiple recipient service provided by the server
            sendThroughService(connection, packet, to, cc, bcc, replyTo, replyRoom, noReply,
                    serviceAddress);
        }
        else {
            // Server does not support XEP-33 so try to send the packet to each recipient
            if (noReply || (replyTo != null && replyTo.trim().length() > 0) ||
                    (replyRoom != null && replyRoom.trim().length() > 0)) {
                // Some specified XEP-33 features were requested so throw an exception alerting
                // the user that this features are not available
                throw new FeatureNotSupportedException("Extended Stanza Addressing");
            }
            // Send the packet to each individual recipient
            sendToIndividualRecipients(connection, packet, to, cc, bcc);
        }
    }

    /**
     * Sends a reply to a previously received stanza(/packet) that was sent to multiple recipients. Before
     * attempting to send the reply message some checkings are performed. If any of those checkings
     * fail then an XMPPException is going to be thrown with the specific error detail.
     *
     * @param connection the connection to use to send the reply.
     * @param original   the previously received stanza(/packet) that was sent to multiple recipients.
     * @param reply      the new message to send as a reply.
     * @throws SmackException 
     * @throws XMPPErrorException 
     */
    public static void reply(XMPPConnection connection, Message original, Message reply) throws SmackException, XMPPErrorException
         {
        MultipleRecipientInfo info = getMultipleRecipientInfo(original);
        if (info == null) {
            throw new SmackException("Original message does not contain multiple recipient info");
        }
        if (info.shouldNotReply()) {
            throw new SmackException("Original message should not be replied");
        }
        if (info.getReplyRoom() != null) {
            throw new SmackException("Reply should be sent through a room");
        }
        // Any <thread/> element from the initial message MUST be copied into the reply.
        if (original.getThread() != null) {
            reply.setThread(original.getThread());
        }
        MultipleAddresses.Address replyAddress = info.getReplyAddress();
        if (replyAddress != null && replyAddress.getJid() != null) {
            // Send reply to the reply_to address
            reply.setTo(replyAddress.getJid());
            connection.sendStanza(reply);
        }
        else {
            // Send reply to multiple recipients
            List<String> to = new ArrayList<String>(info.getTOAddresses().size());
            List<String> cc = new ArrayList<String>(info.getCCAddresses().size());
            for (MultipleAddresses.Address jid : info.getTOAddresses()) {
                to.add(jid.getJid());
            }
            for (MultipleAddresses.Address jid : info.getCCAddresses()) {
                cc.add(jid.getJid());
            }
            // Add original sender as a 'to' address (if not already present)
            if (!to.contains(original.getFrom()) && !cc.contains(original.getFrom())) {
                to.add(original.getFrom());
            }
            // Remove the sender from the TO/CC list (try with bare JID too)
            String from = connection.getUser();
            if (!to.remove(from) && !cc.remove(from)) {
                String bareJID = XmppStringUtils.parseBareJid(from);
                to.remove(bareJID);
                cc.remove(bareJID);
            }

            send(connection, reply, to, cc, null, null, null, false);
        }
    }

    /**
     * Returns the {@link MultipleRecipientInfo} contained in the specified stanza(/packet) or
     * <tt>null</tt> if none was found. Only packets sent to multiple recipients will
     * contain such information.
     *
     * @param packet the stanza(/packet) to check.
     * @return the MultipleRecipientInfo contained in the specified stanza(/packet) or <tt>null</tt>
     *         if none was found.
     */
    public static MultipleRecipientInfo getMultipleRecipientInfo(Stanza packet) {
        MultipleAddresses extension = (MultipleAddresses) packet
                .getExtension(MultipleAddresses.ELEMENT, MultipleAddresses.NAMESPACE);
        return extension == null ? null : new MultipleRecipientInfo(extension);
    }

    private static void sendToIndividualRecipients(XMPPConnection connection, Stanza packet,
            Collection<String> to, Collection<String> cc, Collection<String> bcc) throws NotConnectedException {
        if (to != null) {
            for (String jid : to) {
                packet.setTo(jid);
                connection.sendStanza(new PacketCopy(packet.toXML()));
            }
        }
        if (cc != null) {
            for (String jid : cc) {
                packet.setTo(jid);
                connection.sendStanza(new PacketCopy(packet.toXML()));
            }
        }
        if (bcc != null) {
            for (String jid : bcc) {
                packet.setTo(jid);
                connection.sendStanza(new PacketCopy(packet.toXML()));
            }
        }
    }

    private static void sendThroughService(XMPPConnection connection, Stanza packet, Collection<String> to,
            Collection<String> cc, Collection<String> bcc, String replyTo, String replyRoom, boolean noReply,
            String serviceAddress) throws NotConnectedException {
        // Create multiple recipient extension
        MultipleAddresses multipleAddresses = new MultipleAddresses();
        if (to != null) {
            for (String jid : to) {
                multipleAddresses.addAddress(MultipleAddresses.Type.to, jid, null, null, false, null);
            }
        }
        if (cc != null) {
            for (String jid : cc) {
                multipleAddresses.addAddress(MultipleAddresses.Type.to, jid, null, null, false, null);
            }
        }
        if (bcc != null) {
            for (String jid : bcc) {
                multipleAddresses.addAddress(MultipleAddresses.Type.bcc, jid, null, null, false, null);
            }
        }
        if (noReply) {
            multipleAddresses.setNoReply();
        }
        else {
            if (replyTo != null && replyTo.trim().length() > 0) {
                multipleAddresses
                        .addAddress(MultipleAddresses.Type.replyto, replyTo, null, null, false, null);
            }
            if (replyRoom != null && replyRoom.trim().length() > 0) {
                multipleAddresses.addAddress(MultipleAddresses.Type.replyroom, replyRoom, null, null,
                        false, null);
            }
        }
        // Set the multiple recipient service address as the target address
        packet.setTo(serviceAddress);
        // Add extension to packet
        packet.addExtension(multipleAddresses);
        // Send the packet
        connection.sendStanza(packet);
    }

    /**
     * Returns the address of the multiple recipients service. To obtain such address service
     * discovery is going to be used on the connected server and if none was found then another
     * attempt will be tried on the server items. The discovered information is going to be
     * cached for 24 hours.
     *
     * @param connection the connection to use for disco. The connected server is going to be
     *                   queried.
     * @return the address of the multiple recipients service or <tt>null</tt> if none was found.
     * @throws NoResponseException if there was no response from the server.
     * @throws XMPPErrorException 
     * @throws NotConnectedException 
     */
    private static String getMultipleRecipienServiceAddress(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        List<String> services = sdm.findServices(MultipleAddresses.NAMESPACE, true, true);
        if (services.size() > 0) {
            return services.get(0);
        }
        return null;
    }

    /**
     * Stanza(/Packet) that holds the XML stanza to send. This class is useful when the same packet
     * is needed to be sent to different recipients. Since using the same stanza(/packet) is not possible
     * (i.e. cannot change the TO address of a queues stanza(/packet) to be sent) then this class was
     * created to keep the XML stanza to send.
     */
    private static class PacketCopy extends Stanza {

        private CharSequence text;

        /**
         * Create a copy of a stanza(/packet) with the text to send. The passed text must be a valid text to
         * send to the server, no validation will be done on the passed text.
         *
         * @param text the whole text of the stanza(/packet) to send
         */
        public PacketCopy(CharSequence text) {
            this.text = text;
        }

        @Override
        public CharSequence toXML() {
            return text;
        }

    }

}
