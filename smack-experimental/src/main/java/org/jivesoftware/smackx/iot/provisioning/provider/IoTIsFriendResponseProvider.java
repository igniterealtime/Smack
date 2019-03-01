/**
 *
 * Copyright 2016-2019 Florian Schmaus
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

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.iot.provisioning.element.IoTIsFriendResponse;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmlpull.v1.XmlPullParser;

public class IoTIsFriendResponseProvider extends IQProvider<IoTIsFriendResponse> {

    @Override
    public IoTIsFriendResponse parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmppStringprepException {
        Jid jid = ParserUtils.getJidAttribute(parser);
        BareJid bareJid = jid.asBareJid();
        boolean result = ParserUtils.getBooleanAttribute(parser, "result");

        return new IoTIsFriendResponse(bareJid, result);
    }

}
