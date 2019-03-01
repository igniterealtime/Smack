/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.push_notifications.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;

import org.jivesoftware.smackx.push_notifications.element.PushNotificationsElements.RemoteDisablingExtension;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Push Notifications Remote Disabling Provider class.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0357.html">XEP-0357: Push
 *      Notifications</a>
 * @author Fernando Ramirez
 */
public class RemoteDisablingProvider extends ExtensionElementProvider<RemoteDisablingExtension> {

    @Override
    public RemoteDisablingExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        Jid userJid = null;
        String node = parser.getAttributeValue("", "node");

        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("affiliation")) {
                    userJid = JidCreate.from(parser.getAttributeValue("", "jid"));

                    String affiliation = parser.getAttributeValue("", "affiliation");
                    if (affiliation == null || !affiliation.equals("none")) {
                        return null;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new RemoteDisablingExtension(node, userJid);
    }

}
