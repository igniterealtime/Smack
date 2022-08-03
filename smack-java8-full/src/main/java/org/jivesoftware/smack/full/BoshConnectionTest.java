/**
 *
 * Copyright 2022 Florian Schmaus.
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
package org.jivesoftware.smack.full;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.packet.Message;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class BoshConnectionTest {

    public static void main(String[] args) throws XMPPException, SmackException, IOException, InterruptedException {
        String jidString = args[0];
        EntityBareJid jid = JidCreate.entityBareFrom(jidString);
        String pass = args[1];

        // SmackConfiguration.DEBUG = true;
        BOSHConfiguration config = BOSHConfiguration.builder()
                        .setUsernameAndPassword(jid.getLocalpart(), pass)
                        .setXmppDomain(jid.asDomainBareJid())
                        .useHttps()
                        .setPort(5443)
                        .setFile("/bosh")
                        .build();
        XMPPBOSHConnection connection = new XMPPBOSHConnection(config);

        connection.connect().login();
        Message message = connection.getStanzaFactory()
                        .buildMessageStanza()
                        .to("flo@geekplace.eu")
                        .setBody("Hi there 2")
                        .build();
        connection.sendStanza(message);
        connection.disconnect();
    }

}
