/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * A Version IQ packet, which is used by XMPP clients to discover version information
 * about the software running at another entity's JID.<p>
 *
 * An example to discover the version of the server:
 * <pre>
 * // Request the version from the server.
 * Version versionRequest = new Version();
 * timeRequest.setType(IQ.Type.GET);
 * timeRequest.setTo("example.com");
 *
 * // Create a packet collector to listen for a response.
 * PacketCollector collector = con.createPacketCollector(
 *                new PacketIDFilter(versionRequest.getPacketID()));
 *
 * con.sendPacket(versionRequest);
 *
 * // Wait up to 5 seconds for a result.
 * IQ result = (IQ)collector.nextResult(5000);
 * if (result != null && result.getType() == IQ.Type.RESULT) {
 *     Version versionResult = (Version)result;
 *     // Do something with result...
 * }</pre><p>
 *
 * @author Gaston Dombiak
 */
public class Version extends IQ {

    private String name;
    private String version;
    private String os;

    /**
     * Returns the natural-language name of the software. This property will always be
     * present in a result.
     *
     * @return the natural-language name of the software.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the natural-language name of the software. This message should only be
     * invoked when parsing the XML and setting the property to a Version instance.
     *
     * @param name the natural-language name of the software.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the specific version of the software. This property will always be
     * present in a result.
     *
     * @return the specific version of the software.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the specific version of the software. This message should only be
     * invoked when parsing the XML and setting the property to a Version instance.
     *
     * @param version the specific version of the software.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the operating system of the queried entity. This property will always be
     * present in a result.
     *
     * @return the operating system of the queried entity.
     */
    public String getOs() {
        return os;
    }

    /**
     * Sets the operating system of the queried entity. This message should only be
     * invoked when parsing the XML and setting the property to a Version instance.
     *
     * @param os operating system of the queried entity.
     */
    public void setOs(String os) {
        this.os = os;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"jabber:iq:version\">");
        if (name != null) {
            buf.append("<name>").append(name).append("</name>");
        }
        if (version != null) {
            buf.append("<version>").append(version).append("</version>");
        }
        if (os != null) {
            buf.append("<os>").append(os).append("</os>");
        }
        buf.append("</query>");
        return buf.toString();
    }
}
