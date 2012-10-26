/**
 * $RCSfile$
 * $Revision: 7329 $
 * $Date: 2007-02-28 20:59:28 -0300 (qua, 28 fev 2007) $
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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.media.PayloadType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Jingle content description.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class JingleContentDescription implements PacketExtension {

    // static

    public static final String NODENAME = "description";

    // non-static

    private final List<JinglePayloadType> payloads = new ArrayList<JinglePayloadType>();

    /**
     * Creates a content description..
     */
    public JingleContentDescription() {
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
    public void addJinglePayloadType(final JinglePayloadType pt) {
        synchronized (payloads) {
            payloads.add(pt);
        }
    }

    /**
     * Adds a list of payloads to the packet.
     *
     * @param pts the payloads to add.
     */
    public void addAudioPayloadTypes(final List<PayloadType.Audio> pts) {
        synchronized (payloads) {
            Iterator<PayloadType.Audio> ptIter = pts.iterator();
            while (ptIter.hasNext()) {
                PayloadType.Audio pt = ptIter.next();
                addJinglePayloadType(new JinglePayloadType.Audio(pt));
            }
        }
    }

    /**
     * Returns an Iterator for the audio payloads in the packet.
     *
     * @return an Iterator for the audio payloads in the packet.
     */
    public Iterator<JinglePayloadType> getJinglePayloadTypes() {
        return Collections.unmodifiableList(getJinglePayloadTypesList()).iterator();
    }

    /**
     * Returns a list for the audio payloads in the packet.
     *
     * @return a list for the audio payloads in the packet.
     */
    public ArrayList<JinglePayloadType> getJinglePayloadTypesList() {
        synchronized (payloads) {
            return new ArrayList<JinglePayloadType>(payloads);
        }
    }

    /**
     * Return the list of Payload types contained in the description.
     *
     * @return a list of PayloadType.Audio
     */
    public ArrayList<PayloadType.Audio> getAudioPayloadTypesList() {
        ArrayList<PayloadType.Audio> result = new ArrayList<PayloadType.Audio>();
        Iterator<JinglePayloadType> jinglePtsIter = getJinglePayloadTypes();

        while (jinglePtsIter.hasNext()) {
            JinglePayloadType jpt = jinglePtsIter.next();
            if (jpt instanceof JinglePayloadType.Audio) {
                JinglePayloadType.Audio jpta = (JinglePayloadType.Audio) jpt;
                result.add((PayloadType.Audio)jpta.getPayloadType());
            }
        }

        return result;
    }

    /**
     * Returns a count of the audio payloads in the Jingle packet.
     *
     * @return the number of audio payloads in the Jingle packet.
     */
    public int getJinglePayloadTypesCount() {
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

                Iterator<JinglePayloadType> pt = payloads.listIterator();
                while (pt.hasNext()) {
                    JinglePayloadType pte = pt.next();
                    buf.append(pte.toXML());
                }
                buf.append("</").append(getElementName()).append(">");
            }
        }

        return buf.toString();
    }

    /**
     * Jingle audio description
     */
    public static class Audio extends JingleContentDescription {

        public static final String NAMESPACE = "urn:xmpp:tmp:jingle:apps:rtp";

        public Audio() {
            super();
        }

        /**
         * Utility constructor, with a JinglePayloadType
         */
        public Audio(final JinglePayloadType pt) {
            super();
            addJinglePayloadType(pt);
        }

        public String getNamespace() {
            return NAMESPACE;
        }
    }

    /**
     * A payload type, contained in a descriptor.
     *
     * @author Alvaro Saurin
     */
    public static class JinglePayloadType {

        public static final String NODENAME = "payload-type";

        private PayloadType payload;

        /**
         * Create a payload type.
         *
         * @param payload the payload
         */
        public JinglePayloadType(final PayloadType payload) {
            super();
            this.payload = payload;
        }

        /**
         * Create an empty payload type.
         */
        public JinglePayloadType() {
            this(null);
        }

        /**
         * Returns the XML element name of the element.
         *
         * @return the XML element name of the element.
         */
        public static String getElementName() {
            return NODENAME;
        }

        /**
         * Get the payload represented.
         *
         * @return the payload
         */
        public PayloadType getPayloadType() {
            return payload;
        }

        /**
         * Set the payload represented.
         *
         * @param payload the payload to set
         */
        public void setPayload(final PayloadType payload) {
            this.payload = payload;
        }

        protected String getChildAttributes() {
            return null;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            if (payload != null) {
                buf.append("<").append(getElementName()).append(" ");

                // We covert here the payload type to XML
                if (payload.getId() != PayloadType.INVALID_PT) {
                    buf.append(" id=\"").append(payload.getId()).append("\"");
                }
                if (payload.getName() != null) {
                    buf.append(" name=\"").append(payload.getName()).append("\"");
                }
                if (payload.getChannels() != 0) {
                    buf.append(" channels=\"").append(payload.getChannels()).append("\"");
                }
                if (getChildAttributes() != null) {
                    buf.append(getChildAttributes());
                }
                buf.append("/>");
            }
            return buf.toString();
        }

        /**
         * Audio payload type element
         */
        public static class Audio extends JinglePayloadType {
            public Audio(final PayloadType.Audio audio) {
                super(audio);
            }

            protected String getChildAttributes() {
                StringBuilder buf = new StringBuilder();
                PayloadType pt = getPayloadType();
                if (pt instanceof PayloadType.Audio) {
					PayloadType.Audio pta = (PayloadType.Audio) pt;

					buf.append(" clockrate=\"").append(pta.getClockRate()).append("\" ");
				}
				return buf.toString();
			}
		}
	}
}
