/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smack.util.rce;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;

import org.jivesoftware.smack.datatypes.UInt16;

public interface RemoteConnectionEndpoint {

    CharSequence getHost();

    UInt16 getPort();

    Collection<? extends InetAddress> getInetAddresses();

    String getDescription();

    class InetSocketAddressCoupling<RCE extends RemoteConnectionEndpoint> {
        private final RCE connectionEndpoint;
        private final InetSocketAddress inetSocketAddress;

        public InetSocketAddressCoupling(RCE connectionEndpoint, InetAddress inetAddress) {
            this.connectionEndpoint = connectionEndpoint;

            UInt16 port = connectionEndpoint.getPort();
            inetSocketAddress = new InetSocketAddress(inetAddress, port.intValue());
        }

        public RCE getRemoteConnectionEndpoint() {
            return connectionEndpoint;
        }

        public InetSocketAddress getInetSocketAddress() {
            return inetSocketAddress;
        }

        @Override
        public String toString() {
            return connectionEndpoint.getDescription() + " (" + inetSocketAddress + ')';
        }
    }
}
