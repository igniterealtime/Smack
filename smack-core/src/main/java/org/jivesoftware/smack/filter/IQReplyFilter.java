/**
 *
 * Copyright 2014 Lars Noschinski
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
package org.jivesoftware.smack.filter;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.util.XmppStringUtils;

/**
 * Filters for packets which are a valid reply to an IQ request.
 * <p>
 * Such a stanza(/packet) must have the same stanza(/packet) id and must be an IQ stanza(/packet) of type
 * <code>RESULT</code> or <code>ERROR</code>. Moreover, it is necessary to check
 * the <code>from</code> address to ignore forged replies.
 * <p>
 * We accept a <code>from</code> address if one of the following is true:
 * <ul>
 * <li>It matches the <code>to</code> address of the request.
 * <li>The <code>to</code> address of the request was empty and the
 * <code>from</code> address matches either the bare jid of the server or the
 * (bare or full jid) of the client.
 * <li>To <code>to</code> was our bare address and the <code>from</code> is empty.
 * </ul>
 * <p>
 * For a discussion of the issues, see the thread "Spoofing of iq ids and
 * misbehaving servers" from 2014-01 on the jdev@jabber.org mailing list
 * and following discussion in February and March.
 *
 * @author Lars Noschinski
 *
 */
public class IQReplyFilter implements StanzaFilter {
    private static final Logger LOGGER = Logger.getLogger(IQReplyFilter.class.getName());

    private final StanzaFilter iqAndIdFilter;
    private final OrFilter fromFilter;
    private final String to;
    private final String local;
    private final String server;
    private final String packetId;

    /**
     * Filters for packets which are a valid reply to an IQ request.
     * <p>
     * Such a stanza(/packet) must have the same stanza(/packet) id and must be an IQ stanza(/packet) of type
     * <code>RESULT</code> or <code>ERROR</code>. Moreover, it is necessary to check
     * the <code>from</code> address to ignore forged replies.
     * <p>
     * We accept a <code>from</code> address if one of the following is true:
     * <ul>
     * <li>It matches the <code>to</code> address of the request.
     * <li>The <code>to</code> address of the request was empty and the
     * <code>from</code> address matches either the bare jid of the server or the
     * (bare or full jid) of the client.
     * <li>To <code>to</code> was our bare address and the <code>from</code> is empty.
     * </ul>
     * <p>
     * For a discussion of the issues, see the thread "Spoofing of iq ids and
     * misbehaving servers" from 2014-01 on the jdev@jabber.org mailing list
     * and following discussion in February and March.
     *
     * @param iqPacket An IQ request. Filter for replies to this packet.
     */
    public IQReplyFilter(IQ iqPacket, XMPPConnection conn) {
        if (!iqPacket.isRequestIQ()) {
            throw new IllegalArgumentException("IQ must be a request IQ, i.e. of type 'get' or 'set'.");
        }
        if (iqPacket.getTo() != null) {
            to = iqPacket.getTo().toLowerCase(Locale.US);
        } else {
            to = null;
        }
        final String localJid = conn.getUser();
        if (localJid == null) {
            throw new IllegalArgumentException("Must have a local (user) JID set. Either you didn't configure one or you where not connected at least once");
        }
        local = localJid.toLowerCase(Locale.US);

        server = conn.getServiceName().toLowerCase(Locale.US);
        packetId = iqPacket.getStanzaId();

        StanzaFilter iqFilter = new OrFilter(IQTypeFilter.ERROR, IQTypeFilter.RESULT);
        StanzaFilter idFilter = new StanzaIdFilter(iqPacket);
        iqAndIdFilter = new AndFilter(iqFilter, idFilter);
        fromFilter = new OrFilter();
        fromFilter.addFilter(FromMatchesFilter.createFull(to));
        if (to == null) {
            fromFilter.addFilter(FromMatchesFilter.createBare(local));
            fromFilter.addFilter(FromMatchesFilter.createFull(server));
        }
        else if (to.equals(XmppStringUtils.parseBareJid(local))) {
            fromFilter.addFilter(FromMatchesFilter.createFull(null));
        }
    }

    @Override
    public boolean accept(Stanza packet) {
        // First filter out everything that is not an IQ stanza and does not have the correct ID set.
        if (!iqAndIdFilter.accept(packet))
            return false;

        // Second, check if the from attributes are correct and log potential IQ spoofing attempts
        if (fromFilter.accept(packet)) {
            return true;
        } else {
            String msg = String.format("Rejected potentially spoofed reply to IQ-packet. Filter settings: "
                            + "packetId=%s, to=%s, local=%s, server=%s. Received packet with from=%s",
                            packetId, to, local, server, packet.getFrom());
            LOGGER.log(Level.WARNING, msg , packet);
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(": iqAndIdFilter (").append(iqAndIdFilter.toString()).append("), ");
        sb.append(": fromFilter (").append(fromFilter.toString()).append(')');
        return sb.toString();
    }
}
