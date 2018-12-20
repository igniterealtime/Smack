/**
 *
 * Copyright 2018 Florian Schmaus.
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
package org.jivesoftware.smack;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class ConnectionConfigurationTest {

    @Test
    public void setIp() {
        DummyConnectionConfiguration.Builder builder = newUnitTestBuilder();

        final String ip = "192.168.0.1";
        builder.setHostAddressByNameOrIp(ip);

        DummyConnectionConfiguration connectionConfiguration = builder.build();
        assertEquals('/' + ip, connectionConfiguration.getHostAddress().toString());
    }

    @Test
    public void setFqdn() {
        DummyConnectionConfiguration.Builder builder = newUnitTestBuilder();

        final String fqdn = "foo.example.org";
        builder.setHostAddressByNameOrIp(fqdn);

        DummyConnectionConfiguration connectionConfiguration = builder.build();
        assertEquals(fqdn, connectionConfiguration.getHost().toString());
    }

    private static DummyConnectionConfiguration.Builder newUnitTestBuilder() {
        DummyConnectionConfiguration.Builder builder = DummyConnectionConfiguration.builder();
        builder.setXmppDomain(JidTestUtil.DOMAIN_BARE_JID_1);
        return builder;
    }

    private static final class DummyConnectionConfiguration extends ConnectionConfiguration {

        protected DummyConnectionConfiguration(Builder builder) {
            super(builder);
        }

        public static Builder builder() {
            return new Builder();
        }

        private static final class Builder
                        extends ConnectionConfiguration.Builder<Builder, DummyConnectionConfiguration> {

            @Override
            public DummyConnectionConfiguration build() {
                return new DummyConnectionConfiguration(this);
            }

            @Override
            protected Builder getThis() {
                return this;
            }

        }
    }
}
