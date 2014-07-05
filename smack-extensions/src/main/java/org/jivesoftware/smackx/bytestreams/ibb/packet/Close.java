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
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a request to close an In-Band Bytestream.
 * 
 * @author Henning Staib
 */
public class Close extends IQ {

    public static final String ELEMENT = "close";

    /* unique session ID identifying this In-Band Bytestream */
    private final String sessionID;

    /**
     * Creates a new In-Band Bytestream close request packet.
     * 
     * @param sessionID unique session ID identifying this In-Band Bytestream
     */
    public Close(String sessionID) {
        if (sessionID == null || "".equals(sessionID)) {
            throw new IllegalArgumentException("Session ID must not be null or empty");
        }
        this.sessionID = sessionID;
        setType(Type.set);
    }

    /**
     * Returns the unique session ID identifying this In-Band Bytestream.
     * 
     * @return the unique session ID identifying this In-Band Bytestream
     */
    public String getSessionID() {
        return sessionID;
    }

    @Override
    public XmlStringBuilder getChildElementXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(ELEMENT);
        xml.xmlnsAttribute(DataPacketExtension.NAMESPACE);
        xml.attribute("sid", sessionID);
        xml.closeEmptyElement();
        return xml;
    }

}
