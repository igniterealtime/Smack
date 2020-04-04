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
package org.jivesoftware.smack.c2s;

import java.util.List;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;

public abstract class XmppClientToServerTransport {

    protected final ModularXmppClientToServerConnectionInternal connectionInternal;

    protected XmppClientToServerTransport(ModularXmppClientToServerConnectionInternal connectionInternal) {
        this.connectionInternal = connectionInternal;
    }

    protected abstract void resetDiscoveredConnectionEndpoints();

    protected abstract List<SmackFuture<LookupConnectionEndpointsResult, Exception>> lookupConnectionEndpoints();

    protected abstract void loadConnectionEndpoints(LookupConnectionEndpointsSuccess lookupConnectionEndpointsSuccess);

    /**
     * Notify the transport that new outgoing data is available. Usually this method does not need to be called
     * explicitly, only if the filters are modified so that they potentially produced new data.
     */
    protected abstract void afterFiltersClosed();

    /**
     * Called by the CloseConnection state.
     */
    protected abstract void disconnect();

    protected abstract void notifyAboutNewOutgoingElements();

    public abstract SSLSession getSslSession();

    public abstract boolean isConnected();

    public boolean isTransportSecured() {
        return getSslSession() != null;
    }

    public abstract Stats getStats();

    public abstract static class Stats {
    }

    protected interface LookupConnectionEndpointsResult {
    }

    protected interface LookupConnectionEndpointsSuccess extends LookupConnectionEndpointsResult {
    }

    public interface LookupConnectionEndpointsFailed extends LookupConnectionEndpointsResult {
        // TODO: Add something like getExceptions() or getConnectionExceptions()?
    }

}
