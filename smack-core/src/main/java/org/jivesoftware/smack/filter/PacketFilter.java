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

package org.jivesoftware.smack.filter;

/**
 * Defines a way to filter packets for particular attributes. Stanza(/Packet) filters are used when
 * constructing stanza(/packet) listeners or collectors -- the filter defines what packets match the criteria
 * of the collector or listener for further stanza(/packet) processing.
 * <p>
 * Several simple filters are pre-defined. These filters can be logically combined for more complex
 * stanza(/packet) filtering by using the {@link org.jivesoftware.smack.filter.AndFilter AndFilter} and
 * {@link org.jivesoftware.smack.filter.OrFilter OrFilter} filters. It's also possible to define
 * your own filters by implementing this interface. The code example below creates a trivial filter
 * for packets with a specific ID (real code should use {@link StanzaIdFilter} instead).
 *
 * <pre>
 * // Use an anonymous inner class to define a stanza(/packet) filter that returns
 * // all packets that have a stanza(/packet) ID of &quot;RS145&quot;.
 * PacketFilter myFilter = new PacketFilter() {
 *     public boolean accept(Packet packet) {
 *         return &quot;RS145&quot;.equals(packet.getStanzaId());
 *     }
 * };
 * // Create a new stanza(/packet) collector using the filter we created.
 * PacketCollector myCollector = packetReader.createPacketCollector(myFilter);
 * </pre>
 *
 * @see org.jivesoftware.smack.PacketCollector
 * @see org.jivesoftware.smack.StanzaListener
 * @author Matt Tucker
 * @deprecated use {@link StanzaFilter}
 */
@Deprecated
public interface PacketFilter extends StanzaFilter {

}
