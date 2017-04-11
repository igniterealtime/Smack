/**
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.jingleold.packet;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jingleold.media.ContentInfo;

/**
 * Jingle content info.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public class JingleContentInfo implements ExtensionElement {

    protected ContentInfo mediaInfoElement;

    private String namespace;

    /**
     * Empty constructor, with no jmf info.
     */
    public JingleContentInfo() {
        this(null);
    }

    /**
     * Constructor with a jmf info.
     *
     * @param mediaInfoElement MediaInfo element
     */
    public JingleContentInfo(final ContentInfo mediaInfoElement) {
        super();
        this.mediaInfoElement = mediaInfoElement;
    }

    /**
     * Get the jmf info element.
     *
     * @return the mediaInfoElement
     */
    public ContentInfo getMediaInfo() {
        return mediaInfoElement;
    }

    /**
     * Get the element name.
     */
    @Override
    public String getElementName() {
        // Media info is supposed to be just a single-word command...
        return getMediaInfo().toString();
    }

    /**
     * Set the name space.
     *
     * @param ns the namespace
     */
    protected void setNamespace(final String ns) {
        namespace = ns;
    }

    /**
     * Get the publilc namespace.
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(getElementName()).append(" xmlns=\"");
        buf.append(getNamespace()).append("\" ");
        buf.append("/>");
        return buf.toString();
    }

    /**
     * Transport part of a Jingle packet.
     */
    public static class Audio extends JingleContentInfo {

        public static final String NAMESPACE = "urn:xmpp:tmp:jingle:apps:rtp";

        public Audio(final ContentInfo mi) {
            super(mi);
            setNamespace(NAMESPACE);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        // Subclasses: specialize the Audio jmf info...

        /**
         * Busy jmf info.
         */
        public static class Busy extends Audio {
            public Busy() {
                super(ContentInfo.Audio.BUSY);
            }
        }

        /**
         * Hold jmf info.
         */
        public static class Hold extends Audio {
            public Hold() {
                super(ContentInfo.Audio.HOLD);
            }
        }

        /**
         * Mute jmf info.
         */
        public static class Mute extends Audio {
            public Mute() {
                super(ContentInfo.Audio.MUTE);
            }
        }

        /**
         * Queued jmf info.
         */
        public static class Queued extends Audio {
            public Queued() {
                super(ContentInfo.Audio.QUEUED);
            }
        }

        /**
         * Ringing jmf info.
         */
        public static class Ringing extends Audio {
            public Ringing() {
                super(ContentInfo.Audio.RINGING);
            }
        }
    }
}
