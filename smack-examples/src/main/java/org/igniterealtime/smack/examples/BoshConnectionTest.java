/**
 *
 * Copyright 2022 Florian Schmaus.
 *
 * This file is part of smack-examples.
 *
 * smack-examples is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.examples;

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
