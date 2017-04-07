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

import java.io.IOException;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses the <b>subscription</b> element out of the pubsub IQ message from 
 * the server as specified in the <a href="http://xmpp.org/extensions/xep-0060.html#schemas-pubsub">subscription schema</a>.
 * 
 * @author Robin Collier
 */
public class SubscriptionProvider extends ExtensionElementProvider<Subscription>
{
    @Override
    public Subscription parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {
        String jid = parser.getAttributeValue(null, "jid");
        String nodeId = parser.getAttributeValue(null, "node");
        String subId = parser.getAttributeValue(null, "subid");
        String state = parser.getAttributeValue(null, "subscription");
        boolean isRequired = false;

        int tag = parser.next();

        if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("subscribe-options"))
        {
            tag = parser.next();

            if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("required"))
                isRequired = true;

            while (tag != XmlPullParser.END_TAG && !parser.getName().equals("subscribe-options")) tag = parser.next();
        }
        while (parser.getEventType() != XmlPullParser.END_TAG) parser.next();
        return new Subscription(jid, nodeId, subId, (state == null ? null : Subscription.State.valueOf(state)), isRequired);
    }

}
