/*
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
package org.jivesoftware.smackx.bytestreams.ibb.packet;

import java.util.Locale;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;

/**
 * Represents a request to open an In-Band Bytestream.
 *
 * @author Henning Staib
 */
public class Open extends IQ {

    public static final String ELEMENT = "open";
    public static final String NAMESPACE = DataPacketExtension.NAMESPACE;

    /* unique session ID identifying this In-Band Bytestream */
    private final String sessionID;

    /* block size in which the data will be fragmented */
    private final int blockSize;

    /* stanza type used to encapsulate the data */
    private final StanzaType stanza;

    /**
     * Creates a new In-Band Bytestream open request packet.
     * <p>
     * The data sent over this In-Band Bytestream will be fragmented in blocks
     * with the given block size. The block size should not be greater than
     * 65535. A recommended default value is 4096.
     * <p>
     * The data can be sent using IQ stanzas or message stanzas.
     *
     * @param sessionID unique session ID identifying this In-Band Bytestream
     * @param blockSize block size in which the data will be fragmented
     * @param stanza stanza type used to encapsulate the data
     */
    @SuppressWarnings("this-escape")
    public Open(String sessionID, int blockSize, StanzaType stanza) {
        super(ELEMENT, NAMESPACE);
        if (sessionID == null || "".equals(sessionID)) {
            throw new IllegalArgumentException("Session ID must not be null or empty");
        }
        if (blockSize <= 0) {
            throw new IllegalArgumentException("Block size must be greater than zero");
        }

        this.sessionID = sessionID;
        this.blockSize = blockSize;
        this.stanza = stanza;
        setType(Type.set);
    }

    /**
     * Creates a new In-Band Bytestream open request packet.
     * <p>
     * The data sent over this In-Band Bytestream will be fragmented in blocks
     * with the given block size. The block size should not be greater than
     * 65535. A recommended default value is 4096.
     * <p>
     * The data will be sent using IQ stanzas.
     *
     * @param sessionID unique session ID identifying this In-Band Bytestream
     * @param blockSize block size in which the data will be fragmented
     */
    public Open(String sessionID, int blockSize) {
        this(sessionID, blockSize, StanzaType.IQ);
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
     * Returns the block size in which the data will be fragmented.
     *
     * @return the block size in which the data will be fragmented
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Returns the stanza type used to encapsulate the data.
     *
     * @return the stanza type used to encapsulate the data
     */
    public StanzaType getStanza() {
        return stanza;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("block-size", Integer.toString(blockSize));
        xml.attribute("sid", sessionID);
        xml.attribute("stanza", stanza.toString().toLowerCase(Locale.US));
        xml.setEmptyElement();
        return xml;
    }

}
