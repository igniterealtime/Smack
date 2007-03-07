/**
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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

/**
 * An IQProvider for transcripts.
 *
 * @author Gaston Dombiak
 */
public class TranscriptProvider implements IQProvider {

    public TranscriptProvider() {
        super();
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        String sessionID = parser.getAttributeValue("", "sessionID");
        List packets = new ArrayList();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("message")) {
                    packets.add(PacketParserUtils.parseMessage(parser));
                }
                else if (parser.getName().equals("presence")) {
                    packets.add(PacketParserUtils.parsePresence(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("transcript")) {
                    done = true;
                }
            }
        }

        return new Transcript(sessionID, packets);
    }
}
