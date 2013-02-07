/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Packet extension that contains the list of addresses that a packet should be sent or was sent.
 *
 * @author Gaston Dombiak
 */
public class MultipleAddresses implements PacketExtension {

    public static final String BCC = "bcc";
    public static final String CC = "cc";
    public static final String NO_REPLY = "noreply";
    public static final String REPLY_ROOM = "replyroom";
    public static final String REPLY_TO = "replyto";
    public static final String TO = "to";


    private List<Address> addresses = new ArrayList<Address>();

    /**
     * Adds a new address to which the packet is going to be sent or was sent.
     *
     * @param type on of the static type (BCC, CC, NO_REPLY, REPLY_ROOM, etc.)
     * @param jid the JID address of the recipient.
     * @param node used to specify a sub-addressable unit at a particular JID, corresponding to
     *             a Service Discovery node.
     * @param desc used to specify human-readable information for this address.
     * @param delivered true when the packet was already delivered to this address.
     * @param uri used to specify an external system address, such as a sip:, sips:, or im: URI.
     */
    public void addAddress(String type, String jid, String node, String desc, boolean delivered,
            String uri) {
        // Create a new address with the specificed configuration
        Address address = new Address(type);
        address.setJid(jid);
        address.setNode(node);
        address.setDescription(desc);
        address.setDelivered(delivered);
        address.setUri(uri);
        // Add the new address to the list of multiple recipients
        addresses.add(address);
    }

    /**
     * Indicate that the packet being sent should not be replied.
     */
    public void setNoReply() {
        // Create a new address with the specificed configuration
        Address address = new Address(NO_REPLY);
        // Add the new address to the list of multiple recipients
        addresses.add(address);
    }

    /**
     * Returns the list of addresses that matches the specified type. Examples of address
     * type are: TO, CC, BCC, etc..
     *
     * @param type Examples of address type are: TO, CC, BCC, etc.
     * @return the list of addresses that matches the specified type.
     */
    public List<Address> getAddressesOfType(String type) {
        List<Address> answer = new ArrayList<Address>(addresses.size());
        for (Iterator<Address> it = addresses.iterator(); it.hasNext();) {
            Address address = (Address) it.next();
            if (address.getType().equals(type)) {
                answer.add(address);
            }
        }

        return answer;
    }

    public String getElementName() {
        return "addresses";
    }

    public String getNamespace() {
        return "http://jabber.org/protocol/address";
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName());
        buf.append(" xmlns=\"").append(getNamespace()).append("\">");
        // Loop through all the addresses and append them to the string buffer
        for (Iterator<Address> i = addresses.iterator(); i.hasNext();) {
            Address address = (Address) i.next();
            buf.append(address.toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    public static class Address {

        private String type;
        private String jid;
        private String node;
        private String description;
        private boolean delivered;
        private String uri;

        private Address(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public String getJid() {
            return jid;
        }

        private void setJid(String jid) {
            this.jid = jid;
        }

        public String getNode() {
            return node;
        }

        private void setNode(String node) {
            this.node = node;
        }

        public String getDescription() {
            return description;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        public boolean isDelivered() {
            return delivered;
        }

        private void setDelivered(boolean delivered) {
            this.delivered = delivered;
        }

        public String getUri() {
            return uri;
        }

        private void setUri(String uri) {
            this.uri = uri;
        }

        private String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<address type=\"");
            // Append the address type (e.g. TO/CC/BCC)
            buf.append(type).append("\"");
            if (jid != null) {
                buf.append(" jid=\"");
                buf.append(jid).append("\"");
            }
            if (node != null) {
                buf.append(" node=\"");
                buf.append(node).append("\"");
            }
            if (description != null && description.trim().length() > 0) {
                buf.append(" desc=\"");
                buf.append(description).append("\"");
            }
            if (delivered) {
                buf.append(" delivered=\"true\"");
            }
            if (uri != null) {
                buf.append(" uri=\"");
                buf.append(uri).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }
    }
}
