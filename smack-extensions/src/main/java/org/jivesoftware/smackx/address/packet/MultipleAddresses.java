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

package org.jivesoftware.smackx.address.packet;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Stanza(/Packet) extension that contains the list of addresses that a stanza(/packet) should be sent or was sent.
 *
 * @author Gaston Dombiak
 */
public class MultipleAddresses implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/address";
    public static final String ELEMENT = "addresses";

    public enum Type {
        bcc,
        cc,
        noreply,
        replyroom,
        replyto,
        to,

        /**
         * The "original from" type used to indicate the real originator of the stanza.
         * <p>
         * This Extended Stanza Addressing type is not specified in XEP-33, but in XEP-45 § 7.2.14 (Example 36).
         * </p>
         */
        ofrom,
    }

    private List<Address> addresses = new ArrayList<Address>();

    /**
     * Adds a new address to which the stanza(/packet) is going to be sent or was sent.
     *
     * @param type on of the static type (BCC, CC, NO_REPLY, REPLY_ROOM, etc.)
     * @param jid the JID address of the recipient.
     * @param node used to specify a sub-addressable unit at a particular JID, corresponding to
     *             a Service Discovery node.
     * @param desc used to specify human-readable information for this address.
     * @param delivered true when the stanza(/packet) was already delivered to this address.
     * @param uri used to specify an external system address, such as a sip:, sips:, or im: URI.
     */
    public void addAddress(Type type, String jid, String node, String desc, boolean delivered,
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
     * Indicate that the stanza(/packet) being sent should not be replied.
     */
    public void setNoReply() {
        // Create a new address with the specificed configuration
        Address address = new Address(Type.noreply);
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
    public List<Address> getAddressesOfType(Type type) {
        List<Address> answer = new ArrayList<Address>(addresses.size());
        for (Address address : addresses) {
            if (address.getType().equals(type)) {
                answer.add(address);
            }
        }

        return answer;
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
    public XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder(this);
        buf.rightAngleBracket();
        // Loop through all the addresses and append them to the string buffer
        for (Address address : addresses) {
            buf.append(address.toXML());
        }
        buf.closeElement(this);
        return buf;
    }

    public static class Address implements NamedElement {

        public static final String ELEMENT = "address";

        private final Type type;
        private String jid;
        private String node;
        private String description;
        private boolean delivered;
        private String uri;

        private Address(Type type) {
            this.type = type;
        }

        public Type getType() {
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

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder buf = new XmlStringBuilder();
            buf.halfOpenElement(this).attribute("type", type);
            buf.optAttribute("jid", jid);
            buf.optAttribute("node", node);
            buf.optAttribute("desc", description);
            if (description != null && description.trim().length() > 0) {
                buf.append(" desc=\"");
                buf.append(description).append("\"");
            }
            buf.optBooleanAttribute("delivered", delivered);
            buf.optAttribute("uri", uri);
            buf.closeEmptyElement();
            return buf;
        }
    }
}
