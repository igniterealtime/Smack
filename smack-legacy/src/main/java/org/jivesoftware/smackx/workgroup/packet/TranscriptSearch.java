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

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.SimpleIQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * IQ stanza for retrieving the transcript search form, submitting the completed search form
 * or retrieving the answer of a transcript search.
 *
 * @author Gaston Dombiak
 */
public class TranscriptSearch extends SimpleIQ {

    /**
    * Element name of the stanza extension.
    */
   public static final String ELEMENT_NAME = "transcript-search";

   /**
    * Namespace of the stanza extension.
    */
   public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

   public TranscriptSearch() {
       super(ELEMENT_NAME, NAMESPACE);
   }

    /**
     * An IQProvider for TranscriptSearch packets.
     *
     * @author Gaston Dombiak
     */
    public static class Provider extends IqProvider<TranscriptSearch> {

        @Override
        public TranscriptSearch parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
            TranscriptSearch answer = new TranscriptSearch();

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    // Parse the packet extension
                    PacketParserUtils.addExtensionElement(answer, parser, xmlEnvironment);
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return answer;
        }
    }
}
