/**
 * $RCSfile: JingleDescription.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:08 $
 *
 * Copyright 2003-2005 Jive Software.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.SmackLogger;
import org.jivesoftware.smackx.jingle.media.PayloadType;

/**
 * Jingle content description.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class JingleDescription implements PacketExtension {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(JingleDescription.class);

	// static

    public static final String NODENAME = "description";

    // non-static

    private final List<PayloadType> payloads = new ArrayList<PayloadType>();

    /**
     * Creates a content description..
     */
    public JingleDescription() {
        super();
    }

    /**
     * Returns the XML element name of the element.
     *
     * @return the XML element name of the element.
     */
    public String getElementName() {
        return NODENAME;
    }

    /**
     * Return the namespace.
     *
     * @return The namespace
     */
    public abstract String getNamespace();

    /**
     * Adds a audio payload type to the packet.
     *
     * @param pt the audio payload type to add.
     */
    public void addPayloadType(final PayloadType pt) {
        synchronized (payloads) {
            if (pt == null) {
                LOGGER.error("Null payload type");
            } else {
                payloads.add(pt);
            }
        }
    }

    /**
     * Adds a list of payloads to the packet.
     *
     * @param pts the payloads to add.
     */
    public void addAudioPayloadTypes(final List<PayloadType> pts) {
        synchronized (payloads) {
            Iterator<PayloadType> ptIter = pts.iterator();
            while (ptIter.hasNext()) {
                PayloadType.Audio pt = (PayloadType.Audio) ptIter.next();
                addPayloadType(new PayloadType.Audio(pt));
            }
        }
    }

    /**
     * Returns an Iterator for the audio payloads in the packet.
     *
     * @return an Iterator for the audio payloads in the packet.
     */
    public Iterator<PayloadType> getPayloadTypes() {
        return Collections.unmodifiableList(getPayloadTypesList()).iterator();
    }

    /**
     * Returns a list for the audio payloads in the packet.
     *
     * @return a list for the audio payloads in the packet.
     */
    public List<PayloadType> getPayloadTypesList() {
        synchronized (payloads) {
            return new ArrayList<PayloadType>(payloads);
        }
    }

    /**
     * Return the list of Payload types contained in the description.
     *
     * @return a list of PayloadType.Audio
     */
    public List<PayloadType> getAudioPayloadTypesList() {
        ArrayList<PayloadType> result = new ArrayList<PayloadType>();
        Iterator<PayloadType> jinglePtsIter = getPayloadTypes();

        while (jinglePtsIter.hasNext()) {
            PayloadType jpt = (PayloadType) jinglePtsIter.next();
            if (jpt instanceof PayloadType.Audio) {
                PayloadType.Audio jpta = (PayloadType.Audio) jpt;
                result.add(jpta);
            }
        }

        return result;
    }

    /**
     * Returns a count of the audio payloads in the Jingle packet.
     *
     * @return the number of audio payloads in the Jingle packet.
     */
    public int getPayloadTypesCount() {
        synchronized (payloads) {
            return payloads.size();
        }
    }

    /**
     * Convert a Jingle description to XML.
     *
     * @return a string with the XML representation
     */
    public String toXML() {
        StringBuilder buf = new StringBuilder();

        synchronized (payloads) {
            if (payloads.size() > 0) {
                buf.append("<").append(getElementName());
                buf.append(" xmlns=\"").append(getNamespace()).append("\" >");

                for (PayloadType payloadType : payloads) {
                    if (payloadType != null) {
                        buf.append(payloadType.toXML());
                    }
                }
                buf.append("</").append(getElementName()).append(">");
            }
        }

        return buf.toString();
    }

    /**
     * Jingle audio description
     */
    public static class Audio extends JingleDescription {

        public static final String NAMESPACE = "urn:xmpp:tmp:jingle:apps:rtp";

        public Audio() {
            super();
        }

        /**
         * Utility constructor, with a PayloadType
         */
        public Audio(final PayloadType pt) {
            super();
            addPayloadType(pt);
        }

        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
