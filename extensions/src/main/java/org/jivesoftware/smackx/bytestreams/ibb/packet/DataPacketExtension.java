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
package org.jivesoftware.smackx.bytestreams.ibb.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;

/**
 * Represents a chunk of data of an In-Band Bytestream within an IQ stanza or a
 * message stanza
 * 
 * @author Henning Staib
 */
public class DataPacketExtension implements PacketExtension {

    /**
     * The element name of the data packet extension.
     */
    public final static String ELEMENT_NAME = "data";

    /* unique session ID identifying this In-Band Bytestream */
    private final String sessionID;

    /* sequence of this packet in regard to the other data packets */
    private final long seq;

    /* the data contained in this packet */
    private final String data;

    private byte[] decodedData;

    /**
     * Creates a new In-Band Bytestream data packet.
     * 
     * @param sessionID unique session ID identifying this In-Band Bytestream
     * @param seq sequence of this packet in regard to the other data packets
     * @param data the base64 encoded data contained in this packet
     */
    public DataPacketExtension(String sessionID, long seq, String data) {
        if (sessionID == null || "".equals(sessionID)) {
            throw new IllegalArgumentException("Session ID must not be null or empty");
        }
        if (seq < 0 || seq > 65535) {
            throw new IllegalArgumentException("Sequence must not be between 0 and 65535");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        this.sessionID = sessionID;
        this.seq = seq;
        this.data = data;
    }

    /**
     * Returns the unique session ID identifying this In-Band Bytestream.
     * 
     * @return the unique session ID identifying this In-Band Bytestream
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Returns the sequence of this packet in regard to the other data packets.
     * 
     * @return the sequence of this packet in regard to the other data packets.
     */
    public long getSeq() {
        return seq;
    }

    /**
     * Returns the data contained in this packet.
     * 
     * @return the data contained in this packet.
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the decoded data or null if data could not be decoded.
     * <p>
     * The encoded data is invalid if it contains bad Base64 input characters or
     * if it contains the pad ('=') character on a position other than the last
     * character(s) of the data. See <a
     * href="http://xmpp.org/extensions/xep-0047.html#sec">XEP-0047</a> Section
     * 6.
     * 
     * @return the decoded data
     */
    public byte[] getDecodedData() {
        // return cached decoded data
        if (this.decodedData != null) {
            return this.decodedData;
        }

        // data must not contain the pad (=) other than end of data
        if (data.matches(".*={1,2}+.+")) {
            return null;
        }

        // decodeBase64 will return null if bad characters are included
        this.decodedData = StringUtils.decodeBase64(data);
        return this.decodedData;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return InBandBytestreamManager.NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<");
        buf.append(getElementName());
        buf.append(" ");
        buf.append("xmlns=\"");
        buf.append(InBandBytestreamManager.NAMESPACE);
        buf.append("\" ");
        buf.append("seq=\"");
        buf.append(seq);
        buf.append("\" ");
        buf.append("sid=\"");
        buf.append(sessionID);
        buf.append("\">");
        buf.append(data);
        buf.append("</");
        buf.append(getElementName());
        buf.append(">");
        return buf.toString();
    }

}
