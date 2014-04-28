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
package org.jivesoftware.smackx.jingle.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.jingle.media.ContentInfo;
import org.jivesoftware.smackx.jingle.packet.JingleContentInfo;
import org.xmlpull.v1.XmlPullParser;

/**
 * Jingle Audio jmf-info provider
 *
 * @author Alvaro Saurin
 */
public class JingleContentInfoProvider implements PacketExtensionProvider {

    /**
     * Creates a new provider. ProviderManager requires that every
     * PacketExtensionProvider has a public, no-argument constructor
     */
    public JingleContentInfoProvider() {
        super();
    }

    public PacketExtension parseExtension(final XmlPullParser parser) throws Exception {
        // This method must be overwritten by subclasses...
        return null;
    }

    /**
     * JingleDescription.Audio info provider
     */
    public static class Audio extends JingleContentInfoProvider {

        private PacketExtension audioInfo;

        /**
         * Empty constructor.
         */
        public Audio() {
            this(null);
        }

        /**
         * Constructor with an audio info.
         *
         * @param audioInfo the jmf info
         */
        public Audio(final PacketExtension audioInfo) {
            super();
            this.audioInfo = audioInfo;
        }

        /**
         * Parse a JingleDescription.Audio extension.
         */
        public PacketExtension parseExtension(final XmlPullParser parser)
                throws Exception {
            PacketExtension result = null;

            if (audioInfo != null) {
                result = audioInfo;
            } else {
                String elementName = parser.getName();

                // Try to get an Audio content info
                ContentInfo mi = ContentInfo.Audio.fromString(elementName);
                if (mi != null) {
                    result = new JingleContentInfo.Audio(mi);
                }
            }
            return result;
        }

        // Sub-elements

        public static class Busy extends Audio {
            public Busy() {
                super(new JingleContentInfo.Audio.Busy());
            }
        }

        public static class Hold extends Audio {
            public Hold() {
                super(new JingleContentInfo.Audio.Hold());
            }
        }

        public static class Mute extends Audio {
            public Mute() {
                super(new JingleContentInfo.Audio.Mute());
            }
        }

        public static class Queued extends Audio {
            public Queued() {
                super(new JingleContentInfo.Audio.Queued());
            }
        }

        public static class Ringing extends Audio {
            public Ringing() {
                super(new JingleContentInfo.Audio.Ringing());
			}
		}
	}
}
