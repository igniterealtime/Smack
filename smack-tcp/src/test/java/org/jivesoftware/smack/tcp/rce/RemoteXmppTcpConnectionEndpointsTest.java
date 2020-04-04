/**
 *
 * Copyright 2018-2020 Florian Schmaus.
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
package org.jivesoftware.smack.tcp.rce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.tcp.rce.RemoteXmppTcpConnectionEndpoints.DomainType;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.util.rce.RemoteConnectionException;

import org.junit.jupiter.api.Test;
import org.minidns.record.A;

public class RemoteXmppTcpConnectionEndpointsTest {

    @Test
    public void simpleDomainTypeTest() {
        DomainType client = DomainType.client;
        assertEquals(RemoteXmppTcpConnectionEndpoints.XMPP_CLIENT_DNS_SRV_PREFIX, client.srvPrefix.ace);

        DomainType server = DomainType.server;
        assertEquals(RemoteXmppTcpConnectionEndpoints.XMPP_SERVER_DNS_SRV_PREFIX, server.srvPrefix.ace);
    }

    @Test
    public void testConnectionException() {
        List<RemoteConnectionException<? extends RemoteConnectionEndpoint>> connectionExceptions = new ArrayList<>();

        {
            A aRr = new A("1.2.3.4");
            UInt16 port = UInt16.from(1234);
            String host = "example.org";
            IpTcpRemoteConnectionEndpoint<A> remoteConnectionEndpoint = new IpTcpRemoteConnectionEndpoint<>(host, port,
                            aRr);
            Exception exception = new Exception("Failed for some reason");

            RemoteConnectionException<IpTcpRemoteConnectionEndpoint<A>> remoteConnectionException = RemoteConnectionException.from(
                            remoteConnectionEndpoint, exception);
            connectionExceptions.add(remoteConnectionException);
        }

        {
            A aRr = new A("1.3.3.7");
            UInt16 port = UInt16.from(5678);
            String host = "other.example.org";
            IpTcpRemoteConnectionEndpoint<A> remoteConnectionEndpoint = new IpTcpRemoteConnectionEndpoint<>(host, port,
                            aRr);
            Exception exception = new Exception("Failed for some other reason");

            RemoteConnectionException<IpTcpRemoteConnectionEndpoint<A>> remoteConnectionException = RemoteConnectionException.from(
                            remoteConnectionEndpoint, exception);
            connectionExceptions.add(remoteConnectionException);
        }

        List<RemoteConnectionEndpointLookupFailure> lookupFailures = Collections.emptyList();
        SmackException.EndpointConnectionException endpointConnectionException = SmackException.EndpointConnectionException.from(
                        lookupFailures, connectionExceptions);

        String message = endpointConnectionException.getMessage();
        assertEquals("The following addresses failed: "
                       + "'RFC 6120 A/AAAA Endpoint + [example.org:1234] (/1.2.3.4:1234)' failed because: java.lang.Exception: Failed for some reason, "
                       + "'RFC 6120 A/AAAA Endpoint + [other.example.org:5678] (/1.3.3.7:5678)' failed because: java.lang.Exception: Failed for some other reason",
                        message);
    }

}
