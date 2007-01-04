package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.packet.JingleContentDescription;
import org.jivesoftware.smackx.packet.JingleContentDescription.JinglePayloadType;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parser for a Jingle description
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class JingleContentDescriptionProvider implements PacketExtensionProvider {

    /**
     * Default constructor
     */
    public JingleContentDescriptionProvider() {
        super();
    }

    /**
     * Parse a iq/jingle/description/payload-type element.
     *
     * @param parser the input to parse
     * @return a payload type element
     * @throws Exception
     */
    protected JinglePayloadType parsePayload(final XmlPullParser parser)
            throws Exception {
        int ptId = 0;
        String ptName;
        int ptChannels = 0;

        try {
            ptId = Integer.parseInt(parser.getAttributeValue("", "id"));
        } catch (Exception e) {
        }

        ptName = parser.getAttributeValue("", "name");

        try {
            ptChannels = Integer.parseInt(parser.getAttributeValue("", "channels"));
        } catch (Exception e) {
        }

        return new JinglePayloadType(new PayloadType(ptId, ptName, ptChannels));
    }

    /**
     * Parse a iq/jingle/description element.
     *
     * @param parser the input to parse
     * @return a description element
     * @throws Exception
     */
    public PacketExtension parseExtension(final XmlPullParser parser) throws Exception {
        boolean done = false;
        JingleContentDescription desc = getInstance();

        while (!done) {
            int eventType = parser.next();
            String name = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(JingleContentDescription.JinglePayloadType.NODENAME)) {
                    desc.addJinglePayloadType(parsePayload(parser));
                } else {
                    throw new Exception("Unknow element \"" + name + "\" in content.");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (name.equals(JingleContentDescription.NODENAME)) {
                    done = true;
                }
            }
        }
        return desc;
    }

    /**
     * Return a new instance of this class. Subclasses must overwrite this
     * method.
     */
    protected abstract JingleContentDescription getInstance();

    /**
     * Jingle audio
     */
    public static class Audio extends JingleContentDescriptionProvider {

        /**
         * Default constructor
         */
        public Audio() {
            super();
        }

        /**
         * Parse an audio payload type.
         */
        public JinglePayloadType parsePayload(final XmlPullParser parser)
                throws Exception {
            JinglePayloadType pte = super.parsePayload(parser);
            PayloadType.Audio pt = new PayloadType.Audio(pte.getPayloadType());
            int ptClockRate = 0;

            try {
                ptClockRate = Integer.parseInt(parser.getAttributeValue("", "clockrate"));
            } catch (Exception e) {
            }
            pt.setClockRate(ptClockRate);

            return new JinglePayloadType.Audio(pt);
        }

        /**
         * Get a new instance of this object.
         */
        protected JingleContentDescription getInstance() {
            return new JingleContentDescription.Audio();
		}
	}
}
