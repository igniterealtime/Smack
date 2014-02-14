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
     * @param data data packet extension containing the encoded data
     */
    public Data(DataPacketExtension data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        this.dataPacketExtension = data;

        /*
         * also set as packet extension so that data packet extension can be
         * retrieved from IQ stanza and message stanza in the same way
         */
        addExtension(data);
        setType(IQ.Type.SET);
    }

    /**
     * Returns the data packet extension.
     * <p>
     * Convenience method for <code>packet.getExtension("data",
     * "http://jabber.org/protocol/ibb")</code>.
     * 
     * @return the data packet extension
     */
    public DataPacketExtension getDataPacketExtension() {
        return this.dataPacketExtension;
    }

    public String getChildElementXML() {
        return this.dataPacketExtension.toXML();
    }

}
