/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.packet;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * IQ packet for retrieving the transcript search form, submiting the completed search form
 * or retrieving the answer of a transcript search.
 *
 * @author Gaston Dombiak
 */
public class TranscriptSearch extends IQ {

    /**
    * Element name of the packet extension.
    */
   public static final String ELEMENT_NAME = "transcript-search";

   /**
    * Namespace of the packet extension.
    */
   public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");
        // Add packet extensions, if any are defined.
        buf.append(getExtensionsXML());
        buf.append("</").append(ELEMENT_NAME).append("> ");

        return buf.toString();
    }

    /**
     * An IQProvider for TranscriptSearch packets.
     *
     * @author Gaston Dombiak
     */
    public static class Provider extends IQProvider<TranscriptSearch> {

        @Override
        public TranscriptSearch parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
            TranscriptSearch answer = new TranscriptSearch();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    // Parse the packet extension
                    answer.addExtension(PacketParserUtils.parsePacketExtension(parser.getName(), parser.getNamespace(), parser));
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return answer;
        }
    }
}
