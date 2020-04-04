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

import org.jivesoftware.smack.util.ToStringUtil;

public final class RemoteConnectionException<RCE extends RemoteConnectionEndpoint> {

    private final RemoteConnectionEndpoint.InetSocketAddressCoupling<RCE> address;
    private final Exception exception;

    public RemoteConnectionException(RCE remoteConnectionEndpoint, InetAddress inetAddress,
                    Exception exception) {
        this(new RemoteConnectionEndpoint.InetSocketAddressCoupling<>(remoteConnectionEndpoint, inetAddress), exception);
    }

    public RemoteConnectionException(RemoteConnectionEndpoint.InetSocketAddressCoupling<RCE> address, Exception exception) {
        this.address = address;
        this.exception = exception;
    }

    public RemoteConnectionEndpoint.InetSocketAddressCoupling<RCE> getAddress() {
        return address;
    }

    public Exception getException() {
        return exception;
    }

    public String getErrorMessage() {
        return "\'" +  address + "' failed because: " + exception;
    }

    private transient String toStringCache;

    @Override
    public String toString() {
        if (toStringCache == null) {
            toStringCache = ToStringUtil.builderFor(RemoteConnectionException.class)
                .addValue("address", address)
                .addValue("exception", exception)
                .build();
        }
        return toStringCache;
    }

    public static <SARCE extends SingleAddressRemoteConnectionEndpoint> RemoteConnectionException<SARCE> from(SARCE remoteConnectionEndpoint, Exception exception) {
        return new RemoteConnectionException<SARCE>(remoteConnectionEndpoint, remoteConnectionEndpoint.getInetAddress(), exception);
    }
}
