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
package org.jivesoftware.smackx.bytestreams.ibb.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * Represents a chunk of data sent over an In-Band Bytestream encapsulated in an
 * IQ stanza.
 * 
 * @author Henning Staib
 */
public class Data extends IQ {

    /* the data packet extension */
    private final DataPacketExtension dataPacketExtension;

    /**
     * Constructor.
     * 
     * @param data data stanza(/packet) extension containing the encoded data
     */
    public Data(DataPacketExtension data) {
        super(DataPacketExtension.ELEMENT, DataPacketExtension.NAMESPACE);
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        this.dataPacketExtension = data;

        setType(IQ.Type.set);
    }

    /**
     * Returns the data stanza(/packet) extension.
     * 
     * @return the data stanza(/packet) extension
     */
    public DataPacketExtension getDataPacketExtension() {
        return this.dataPacketExtension;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        return dataPacketExtension.getIQChildElementBuilder(xml);
    }

}
