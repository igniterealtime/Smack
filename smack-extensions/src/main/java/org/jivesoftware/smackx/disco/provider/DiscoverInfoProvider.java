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

package org.jivesoftware.smackx.disco.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.xmlpull.v1.XmlPullParser;

/**
* The DiscoverInfoProvider parses Service Discovery information packets.
*
* @author Gaston Dombiak
*/
public class DiscoverInfoProvider extends IQProvider<DiscoverInfo> {

    @Override
    public DiscoverInfo parse(XmlPullParser parser, int initialDepth)
                    throws Exception {
        DiscoverInfo discoverInfo = new DiscoverInfo();
        boolean done = false;
        DiscoverInfo.Identity identity = null;
        String category = "";
        String name = "";
        String type = "";
        String variable = "";
        String lang = "";
        discoverInfo.setNode(parser.getAttributeValue("", "node"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("identity")) {
                    // Initialize the variables from the parsed XML
                    category = parser.getAttributeValue("", "category");
                    name = parser.getAttributeValue("", "name");
                    type = parser.getAttributeValue("", "type");
                    lang = parser.getAttributeValue(parser.getNamespace("xml"), "lang");
                }
                else if (parser.getName().equals("feature")) {
                    // Initialize the variables from the parsed XML
                    variable = parser.getAttributeValue("", "var");
                }
                // Otherwise, it must be a packet extension.
                else {
                    PacketParserUtils.addExtensionElement(discoverInfo, parser);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("identity")) {
                    // Create a new identity and add it to the discovered info.
                    identity = new DiscoverInfo.Identity(category, type, name, lang);
                    discoverInfo.addIdentity(identity);
                }
                if (parser.getName().equals("feature")) {
                    // Create a new feature and add it to the discovered info.
                    boolean notADuplicateFeature = discoverInfo.addFeature(variable);
                    assert(notADuplicateFeature);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return discoverInfo;
    }
}
