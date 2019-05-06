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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.minidns.DnsCache;
import org.minidns.DnsClient;
import org.minidns.MiniDnsFuture;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.DnsMessage.RESPONSE_CODE;
import org.minidns.dnsmessage.Question;
import org.minidns.dnsname.DnsName;
import org.minidns.dnsqueryresult.CachedDnsQueryResult;
import org.minidns.dnsqueryresult.DnsQueryResult;
import org.minidns.dnssec.DnssecClient;
import org.minidns.dnssec.DnssecValidationFailedException;
import org.minidns.record.Record.TYPE;
import org.minidns.source.DnsDataSource;

public final class DnsOverXmppMiniDnsResolverTest {

    @Test
    public void dnsOverXmppMiniDnsResolverTest() throws IOException {
        TestDnsDataSource dnsSource = new TestDnsDataSource();
        TestDnsDataSource dnssecSource = new TestDnsDataSource();

        DnsClient dnsClient = new DnsClient(NoopDnsCache.INSTANCE);
        dnsClient.setDataSource(dnsSource);

        DnssecClient dnssecClient = new DnssecClient(NoopDnsCache.INSTANCE);
        dnssecClient.setDataSource(dnssecSource);

        DnsOverXmppMiniDnsResolver doxResolver = new DnsOverXmppMiniDnsResolver(dnsClient, dnssecClient);

        Question question = new Question("example.org", TYPE.A);

        {
            DnsMessage nondnssecQuery = question.asQueryMessage();

            doxResolver.resolve(nondnssecQuery);

            assertTrue(dnsSource.getAndResetWasQueried());
            assertFalse(dnssecSource.getAndResetWasQueried());
        }

        {
            DnsMessage.Builder dnssecQueryBuilder = question.asMessageBuilder();
            dnssecQueryBuilder.getEdnsBuilder().setDnssecOk();
            DnsMessage dnssecQuery = dnssecQueryBuilder.build();

            DnssecValidationFailedException dnssecValidationFailedException = null;
            try {
                doxResolver.resolve(dnssecQuery);
            } catch (DnssecValidationFailedException e) {
                dnssecValidationFailedException = e;
            }
            // This exception is expected since we don't have a realy DNS source.
            assertNotNull(dnssecValidationFailedException);

            assertFalse(dnsSource.getAndResetWasQueried());
            assertTrue(dnssecSource.getAndResetWasQueried());
        }
    }

    public static class TestDnsDataSource implements DnsDataSource {

        private final AtomicBoolean wasQueried = new AtomicBoolean();

        public boolean getAndResetWasQueried() {
            return wasQueried.getAndSet(false);
        }

        private void setWasQueried() {
            wasQueried.set(true);
        }

        @Override
        public DnsQueryResult query(DnsMessage query, InetAddress address, int port) throws IOException {
            setWasQueried();
            return new TestDnsQueryResult(query);
        }

        @Override
        public MiniDnsFuture<DnsQueryResult, IOException> queryAsync(DnsMessage query, InetAddress address, int port,
                OnResponseCallback onResponseCallback) {
            setWasQueried();
            DnsQueryResult result = new TestDnsQueryResult(query);
            return MiniDnsFuture.from(result);
        }

        @Override
        public int getUdpPayloadSize() {
            return 0;
        }

        @Override
        public int getTimeout() {
            return 0;
        }

        @Override
        public void setTimeout(int timeout) {
        }

        private static class TestDnsQueryResult extends DnsQueryResult {

            protected TestDnsQueryResult(DnsMessage query) {
                super(QueryMethod.testWorld, query, createNxDomainAnswerFor(query));
            }

            private static DnsMessage createNxDomainAnswerFor(DnsMessage query) {
                Question question = query.getQuestion();

                DnsMessage response = DnsMessage.builder()
                    .setQuestion(question)
                    .setRecursionAvailable(true)
                    .setResponseCode(RESPONSE_CODE.NX_DOMAIN)
                    .build();

                return response;
            }
        }
    }

    // TODO: Workaround for NPE-if-no-cache-set bug in MiniDNS. Remove we use a MiniDNS version where this is fixed,
    // i.e. one that has 864fbb5 ("Fix NPE in AbstractDnsClient if cache is 'null'")
    private static class NoopDnsCache extends DnsCache {

        private static final NoopDnsCache INSTANCE = new NoopDnsCache();

        @Override
        protected void putNormalized(DnsMessage normalizedQuery, DnsQueryResult result) {
        }

        @Override
        public void offer(DnsMessage query, DnsQueryResult result, DnsName authoritativeZone) {
        }

        @Override
        protected CachedDnsQueryResult getNormalized(DnsMessage normalizedQuery) {
            return null;
        }

    }
}
