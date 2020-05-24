/**
 *
 * Copyright 2020 Florian Schmaus.
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

import java.net.InetAddress;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.util.rce.SingleAddressRemoteConnectionEndpoint;

import org.minidns.record.InternetAddressRR;

public final class IpTcpRemoteConnectionEndpoint<IARR extends InternetAddressRR<?>>
                implements Rfc6120TcpRemoteConnectionEndpoint, SingleAddressRemoteConnectionEndpoint {

    private final CharSequence host;

    private final UInt16 port;

    private final IARR internetAddressResourceRecord;

    public IpTcpRemoteConnectionEndpoint(CharSequence host, UInt16 port, IARR internetAddressResourceRecord) {
        this.host = host;
        this.port = port;
        this.internetAddressResourceRecord = internetAddressResourceRecord;
    }

    public static IpTcpRemoteConnectionEndpoint<InternetAddressRR<?>> from(CharSequence host, UInt16 port,
                    InetAddress inetAddress) {
        InternetAddressRR<?> internetAddressResourceRecord = InternetAddressRR.from(inetAddress);

        return new IpTcpRemoteConnectionEndpoint<InternetAddressRR<?>>(host, port,
                        internetAddressResourceRecord);
    }

    @Override
    public CharSequence getHost() {
        return host;
    }

    @Override
    public UInt16 getPort() {
        return port;
    }

    @Override
    public InetAddress getInetAddress() {
        return internetAddressResourceRecord.getInetAddress();
    }

    @Override
    public String getDescription() {
        return "RFC 6120 A/AAAA Endpoint + [" + host + ":" + port + "]";
    }
}
