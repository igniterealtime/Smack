/**
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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.DataInputStream;
import java.io.IOException;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

/**
 * A collection of utility methods for SOcKS5 messages.
 * 
 * @author Henning Staib
 */
class Socks5Utils {

    /**
     * Returns a SHA-1 digest of the given parameters as specified in <a
     * href="http://xmpp.org/extensions/xep-0065.html#impl-socks5">XEP-0065</a>.
     * 
     * @param sessionID for the SOCKS5 Bytestream
     * @param initiatorJID JID of the initiator of a SOCKS5 Bytestream
     * @param targetJID JID of the target of a SOCKS5 Bytestream
     * @return SHA-1 digest of the given parameters
     */
    public static String createDigest(String sessionID, String initiatorJID, String targetJID) {
        StringBuilder b = new StringBuilder();
        b.append(sessionID).append(initiatorJID).append(targetJID);
        return StringUtils.hash(b.toString());
    }

    /**
     * Reads a SOCKS5 message from the given InputStream. The message can either be a SOCKS5 request
     * message or a SOCKS5 response message.
     * <p>
     * (see <a href="http://tools.ietf.org/html/rfc1928">RFC1928</a>)
     * 
     * @param in the DataInputStream to read the message from
     * @return the SOCKS5 message
     * @throws IOException if a network error occurred
     * @throws XMPPException if the SOCKS5 message contains an unsupported address type
     */
    public static byte[] receiveSocks5Message(DataInputStream in) throws IOException, XMPPException {
        byte[] header = new byte[5];
        in.readFully(header, 0, 5);

        if (header[3] != (byte) 0x03) {
            throw new XMPPException("Unsupported SOCKS5 address type");
        }

        int addressLength = header[4];

        byte[] response = new byte[7 + addressLength];
        System.arraycopy(header, 0, response, 0, header.length);

        in.readFully(response, header.length, addressLength + 2);

        return response;
    }

}
