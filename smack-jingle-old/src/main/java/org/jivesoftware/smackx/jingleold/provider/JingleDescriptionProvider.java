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
package org.jivesoftware.smackx.jingleold.provider;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.packet.JingleDescription;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parser for a Jingle description.
 * 
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class JingleDescriptionProvider extends ExtensionElementProvider<JingleDescription> {

    /**
     * Parse a iq/jingle/description/payload-type element.
     * 
     * @param parser
     *            the input to parse
     * @return a payload type element
     */
    protected PayloadType parsePayload(final XmlPullParser parser) {
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

        return new PayloadType(ptId, ptName, ptChannels);
    }

    /**
     * Parse a iq/jingle/description element.
     * 
     * @param parser
     *            the input to parse
     * @return a description element
     * @throws SmackException 
     * @throws IOException 
     * @throws XmlPullParserException 
     */
    @Override
    public JingleDescription parse(XmlPullParser parser, int initialDepth) throws SmackException, XmlPullParserException, IOException {
        boolean done = false;
        JingleDescription desc = getInstance();

        while (!done) {
            int eventType = parser.next();
            String name = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(PayloadType.NODENAME)) {
                    desc.addPayloadType(parsePayload(parser));
                } else {
                    throw new SmackException("Unknow element \"" + name + "\" in content.");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (name.equals(JingleDescription.NODENAME)) {
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
    protected abstract JingleDescription getInstance();

    /**
     * Jingle audio.
     */
    public static class Audio extends JingleDescriptionProvider {

        /**
         * Parse an audio payload type.
         */
        @Override
        public PayloadType parsePayload(final XmlPullParser parser) {
            PayloadType pte = super.parsePayload(parser);
            PayloadType.Audio pt = new PayloadType.Audio(pte);
            int ptClockRate = 0;

            try {
                ptClockRate = Integer.parseInt(parser.getAttributeValue("", "clockrate"));
            } catch (Exception e) {
            }
            pt.setClockRate(ptClockRate);

            return pt;
        }

        /**
         * Get a new instance of this object.
         */
        @Override
        protected JingleDescription getInstance() {
            return new JingleDescription.Audio();
        }
    }
}
