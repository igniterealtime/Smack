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
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;

import org.minidns.record.SRV;

public abstract class SrvRemoteConnectionEndpoint implements RemoteConnectionEndpoint {

    protected final SRV srv;

    protected final UInt16 port;

    private final List<? extends InetAddress> inetAddresses;

    protected SrvRemoteConnectionEndpoint(SRV srv, List<? extends InetAddress> inetAddresses) {
        this.srv = srv;
        this.port = UInt16.from(srv.port);
        this.inetAddresses = Objects.requireNonNull(inetAddresses);
    }

    @Override
    public final CharSequence getHost() {
        return srv.target;
    }

    @Override
    public final UInt16 getPort() {
        return port;
    }

    @Override
    public final Collection<? extends InetAddress> getInetAddresses() {
        return inetAddresses;
    }

}
