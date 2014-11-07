/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.bytestreams.socks5;

import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;

/**
 * A collection of utility methods to create XMPP packets.
 * 
 * @author Henning Staib
 */
public class Socks5PacketUtils {

    /**
     * Returns a SOCKS5 Bytestream initialization request packet. The Request doesn't contain any
     * SOCKS5 proxies.
     * 
     * @param from the initiator
     * @param to the target
     * @param sessionID the session ID
     * @return SOCKS5 Bytestream initialization request packet
     */
    public static Bytestream createBytestreamInitiation(String from, String to, String sessionID) {
        Bytestream bytestream = new Bytestream();
        bytestream.setFrom(from);
        bytestream.setTo(to);
        bytestream.setSessionID(sessionID);
        bytestream.setType(IQ.Type.set);
        return bytestream;
    }

    /**
     * Returns a response to a SOCKS5 Bytestream initialization request. The packet doesn't contain
     * the uses-host information.
     * 
     * @param from the target
     * @param to the initiator
     * @return response to a SOCKS5 Bytestream initialization request
     */
    public static Bytestream createBytestreamResponse(String from, String to) {
        Bytestream streamHostInfo = new Bytestream();
        streamHostInfo.setFrom(from);
        streamHostInfo.setTo(to);
        streamHostInfo.setType(IQ.Type.result);
        return streamHostInfo;
    }

    /**
     * Returns a response to an item discovery request. The packet doesn't contain any items.
     * 
     * @param from the XMPP server
     * @param to the XMPP client
     * @return response to an item discovery request
     */
    public static DiscoverItems createDiscoverItems(String from, String to) {
        DiscoverItems discoverItems = new DiscoverItems();
        discoverItems.setFrom(from);
        discoverItems.setTo(to);
        discoverItems.setType(IQ.Type.result);
        return discoverItems;
    }

    /**
     * Returns a response to an info discovery request. The packet doesn't contain any infos.
     * 
     * @param from the target
     * @param to the initiator
     * @return response to an info discovery request
     */
    public static DiscoverInfo createDiscoverInfo(String from, String to) {
        DiscoverInfo discoverInfo = new DiscoverInfo();
        discoverInfo.setFrom(from);
        discoverInfo.setTo(to);
        discoverInfo.setType(IQ.Type.result);
        return discoverInfo;
    }

    /**
     * Returns a response IQ for a activation request to the proxy.
     * 
     * @param from JID of the proxy
     * @param to JID of the client who wants to activate the SOCKS5 Bytestream
     * @return response IQ for a activation request to the proxy
     */
    public static IQ createActivationConfirmation(String from, String to) {
        IQ response = new EmptyResultIQ();
        response.setFrom(from);
        response.setTo(to);
        return response;
    }

}
