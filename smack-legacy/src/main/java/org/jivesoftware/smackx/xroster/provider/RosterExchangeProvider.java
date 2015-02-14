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

package org.jivesoftware.smackx.xroster.provider;

import java.io.IOException;
import java.util.ArrayList;

import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.xroster.RemoteRosterEntry;
import org.jivesoftware.smackx.xroster.packet.RosterExchange;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * The RosterExchangeProvider parses RosterExchange packets.
 *
 * @author Gaston Dombiak
 */
public class RosterExchangeProvider extends PacketExtensionProvider<RosterExchange> {

    /**
     * Parses a RosterExchange packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException 
     * @throws XmlPullParserException 
     */
    @Override
    public RosterExchange parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {

        RosterExchange rosterExchange = new RosterExchange();
        boolean done = false;
        RemoteRosterEntry remoteRosterEntry = null;
        Jid jid = null;
		String name = "";
		ArrayList<String> groupsName = new ArrayList<String>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                	// Reset this variable since they are optional for each item
					groupsName = new ArrayList<String>();
					// Initialize the variables from the parsed XML
                    jid = ParserUtils.getJidAttribute(parser);
                    name = parser.getAttributeValue("", "name");
                }
                if (parser.getName().equals("group")) {
					groupsName.add(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
					// Create packet.
					remoteRosterEntry = new RemoteRosterEntry(jid, name, (String[]) groupsName.toArray(new String[groupsName.size()]));
                    rosterExchange.addRosterEntry(remoteRosterEntry);
                }
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return rosterExchange;

    }

}
