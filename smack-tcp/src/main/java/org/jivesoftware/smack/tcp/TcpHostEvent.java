/**
 *
 * Copyright 2019-2020 Florian Schmaus
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
package org.jivesoftware.smack.tcp;

import java.io.IOException;

import org.jivesoftware.smack.fsm.ConnectionStateEvent.DetailedTransitionIntoInformation;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.tcp.rce.Rfc6120TcpRemoteConnectionEndpoint;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;

public abstract class TcpHostEvent extends DetailedTransitionIntoInformation {
    protected final RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address;

    protected TcpHostEvent(State state, RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address) {
        super(state);
        this.address = address;
    }

    public RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + address;
    }

    public static final class ConnectingToHostEvent extends TcpHostEvent {
        ConnectingToHostEvent(State state,
                        RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address) {
            super(state, address);
        }
    }

    public static final class ConnectedToHostEvent extends TcpHostEvent {
        private final boolean connectionEstablishedImmediately;

        ConnectedToHostEvent(State state, RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address, boolean immediately) {
            super(state, address);
            this.connectionEstablishedImmediately = immediately;
        }

        @Override
        public String toString() {
            return super.toString() + (connectionEstablishedImmediately ? "" : " not") + " connected immediately";
        }
    }

    public static final class ConnectionToHostFailedEvent extends TcpHostEvent {
        private final IOException ioException;

        ConnectionToHostFailedEvent(State state,
                        RemoteConnectionEndpoint.InetSocketAddressCoupling<Rfc6120TcpRemoteConnectionEndpoint> address,
                        IOException ioException) {
            super(state, address);
            this.ioException = ioException;
        }

        @Override
        public String toString() {
            return super.toString() + ioException;
        }
    }
}
