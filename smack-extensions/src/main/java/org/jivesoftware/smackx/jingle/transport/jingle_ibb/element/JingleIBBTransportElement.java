/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb.element;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;

/**
 * Transport Element for JingleInBandBytestream transports.
 */
public class JingleIBBTransportElement extends JingleContentTransportElement {
    public static final String ATTR_BLOCK_SIZE = "block-size";
    public static final String ATTR_SID = "sid";

    private final String sid;
    private final Short blockSize;

    public JingleIBBTransportElement(String streamId, Short blockSize) {
        super(null);
        this.sid = streamId;
        this.blockSize = blockSize != null ? blockSize : JingleIBBTransport.DEFAULT_BLOCK_SIZE;
    }

    public Short getBlockSize() {
        return blockSize;
    }

    public String getStreamId() {
        return sid;
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.attribute(ATTR_BLOCK_SIZE, blockSize);
        xml.attribute(ATTR_SID, sid);
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof JingleIBBTransportElement)) {
            return false;
        }

        return this == other || this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return this.toXML(null).toString().hashCode();
    }
}
