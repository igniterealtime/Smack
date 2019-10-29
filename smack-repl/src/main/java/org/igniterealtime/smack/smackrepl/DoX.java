/**
 *
 * Copyright 2019 Florian Schmaus
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
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
package org.igniterealtime.smack.smackrepl;

import java.io.IOException;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import org.jivesoftware.smackx.dox.DnsOverXmppManager;
import org.jivesoftware.smackx.dox.resolver.minidns.DnsOverXmppMiniDnsResolver;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;
import org.minidns.record.Record;

public class DoX {

    public static void main(String[] args) throws XMPPException, SmackException, IOException, InterruptedException {
        SmackConfiguration.DEBUG = true;

        XMPPTCPConnection connection = new XMPPTCPConnection(args[0], args[1]);
        connection.setReplyTimeout(60000);

        connection.connect().login();

        DnsOverXmppManager dox = DnsOverXmppManager.getInstanceFor(connection);

        Jid target = JidCreate.from("dns@moparisthebest.com/listener");
        Question question = new Question("geekplace.eu", Record.TYPE.A);

        DnsMessage response = dox.query(target, question);

        // CHECKSTYLE:OFF
        System.out.println(response);
        // CHECKSTYLE:ON

        connection.disconnect();
    }

    public static XMPPTCPConnection runDoxResolver(String jid, String password)
            throws XMPPException, SmackException, IOException, InterruptedException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setXmppAddressAndPassword(jid, password)
                .setResource("dns")
                .setDebuggerFactory(ConsoleDebugger.Factory.INSTANCE)
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(config);

        connection.connect().login();

        DnsOverXmppManager dox = DnsOverXmppManager.getInstanceFor(connection);
        dox.setDnsOverXmppResolver(DnsOverXmppMiniDnsResolver.INSTANCE);

        dox.enable();

        return connection;
    }
}
