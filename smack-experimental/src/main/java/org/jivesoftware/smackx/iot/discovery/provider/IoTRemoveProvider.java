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
package org.jivesoftware.smackx.iot.discovery.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.iot.discovery.element.IoTRemove;
import org.jivesoftware.smackx.iot.element.NodeInfo;
import org.jivesoftware.smackx.iot.parser.NodeInfoParser;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class IoTRemoveProvider extends IQProvider<IoTRemove> {

    @Override
    public IoTRemove parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException {
        Jid jid = ParserUtils.getJidAttribute(parser);
        if (jid.hasResource()) {
            // TODO: Should be SmackParseException.
            throw new IOException("JID must be without resourcepart");
        }
        BareJid bareJid = jid.asBareJid();
        NodeInfo nodeInfo = NodeInfoParser.parse(parser);
        return new IoTRemove(bareJid, nodeInfo);
    }

}
