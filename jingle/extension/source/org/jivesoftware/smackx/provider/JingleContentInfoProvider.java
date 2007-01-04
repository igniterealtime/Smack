package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.jingle.media.ContentInfo;
import org.jivesoftware.smackx.packet.JingleContentInfo;
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
     * JingleContentDescription.Audio info provider
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
         * Parse a JingleContentDescription.Audio extension.
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
