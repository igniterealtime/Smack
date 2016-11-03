/**
 *
 * Copyright © 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.provisioning.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.iot.provisioning.element.Friend;
import org.jxmpp.jid.BareJid;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmlpull.v1.XmlPullParser;

public class FriendProvider extends ExtensionElementProvider<Friend> {

    @Override
    public Friend parse(XmlPullParser parser, int initialDepth) throws XmppStringprepException {
        BareJid jid = ParserUtils.getBareJidAttribute(parser);
        return new Friend(jid);
    }

}
