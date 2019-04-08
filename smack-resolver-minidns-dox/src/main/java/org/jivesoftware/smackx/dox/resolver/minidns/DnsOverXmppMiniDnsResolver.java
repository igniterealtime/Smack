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
package org.jivesoftware.smackx.dox.resolver.minidns;

import java.io.IOException;

import org.jivesoftware.smackx.dox.DnsOverXmppResolver;

import org.minidns.DnsClient;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;
import org.minidns.dnsqueryresult.DnsQueryResult;

public class DnsOverXmppMiniDnsResolver implements DnsOverXmppResolver {

    public static final DnsOverXmppMiniDnsResolver INSTANCE = new DnsOverXmppMiniDnsResolver(new DnsClient());

    private final DnsClient dnsClient;

    public DnsOverXmppMiniDnsResolver(DnsClient dnsClient) {
        this.dnsClient = dnsClient;
    }

    @Override
    public DnsMessage resolve(DnsMessage query) throws IOException {
        Question question = query.getQuestion();

        DnsQueryResult dnsQueryResult = dnsClient.query(question);

        return dnsQueryResult.response;
    }

}
