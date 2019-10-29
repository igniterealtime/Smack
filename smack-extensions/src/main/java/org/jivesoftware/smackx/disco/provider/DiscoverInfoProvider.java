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

import java.io.IOException;

import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfoBuilder;

/**
* The DiscoverInfoProvider parses Service Discovery information packets.
*
* @author Gaston Dombiak
*/
public class DiscoverInfoProvider extends IqProvider<DiscoverInfo> {

    @Override
    public DiscoverInfo parse(XmlPullParser parser, int initialDepth, IqBuilder iqData, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        DiscoverInfoBuilder discoverInfoBuilder = DiscoverInfo.builder(iqData);

        String node = parser.getAttributeValue("node");
        discoverInfoBuilder.setNode(node);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                final String name = parser.getName();
                final String namespace = parser.getNamespace();
                if (namespace.equals(DiscoverInfo.NAMESPACE)) {
                    switch (name) {
                    case "identity":
                        String category = parser.getAttributeValue("category");
                        String identityName = parser.getAttributeValue("name");
                        String type = parser.getAttributeValue("type");
                        String lang = ParserUtils.getXmlLang(parser);
                        DiscoverInfo.Identity identity = new DiscoverInfo.Identity(category, type, identityName, lang);
                        discoverInfoBuilder.addIdentity(identity);
                        break;
                    case "feature":
                        String feature = parser.getAttributeValue("var");
                        discoverInfoBuilder.addFeature(feature);
                        break;
                    }
                }
                // Otherwise, it must be a packet extension.
                else {
                    PacketParserUtils.addExtensionElement(discoverInfoBuilder, parser, xmlEnvironment);
                }
            } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        DiscoverInfo discoverInfo = discoverInfoBuilder.buildWithoutValidiation();
        return discoverInfo;
    }
}
