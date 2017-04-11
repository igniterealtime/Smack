/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses the root pubsub stanza(/packet) extensions of the {@link IQ} stanza(/packet) and returns
 * a {@link PubSub} instance.
 * 
 * @author Robin Collier
 */
public class PubSubProvider extends IQProvider<PubSub>
{
    @Override
    public PubSub parse(XmlPullParser parser, int initialDepth)
                    throws Exception {
        String namespace = parser.getNamespace();
        PubSubNamespace pubSubNamespace = PubSubNamespace.valueOfFromXmlns(namespace);
        PubSub pubsub = new PubSub(pubSubNamespace);

        outerloop: while (true) 
        {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                PacketParserUtils.addExtensionElement(pubsub, parser);
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return pubsub;
    }
}
