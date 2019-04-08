/**
 *
 * Copyright 2019 Florian Schmaus
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
