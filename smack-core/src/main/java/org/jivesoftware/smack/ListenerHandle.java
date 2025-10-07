/*
 *
 * Copyright 2024 Florian Schmaus
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

/**
 * A handle of a listener, typically added to a connection. This handle implements {@link AutoCloseable},
 * allowing the the listener to be added using a try-with-resources statement.
 */
public abstract class ListenerHandle implements AutoCloseable {

    protected final XMPPConnection connection;
    protected final StanzaListener listener;

    private ListenerHandle(XMPPConnection connection, StanzaListener listener) {
        this.connection = connection;
        this.listener = listener;
    }

    /**
     * Remove the handle's listener from the connection.
     * @return true if the stanza listener was removed
     */
    public abstract boolean removeListener();

    @Override
    public final void close() {
        removeListener();
    }

    static class StanzaListenerHandle extends ListenerHandle {
        StanzaListenerHandle(XMPPConnection connection, StanzaListener listener) {
            super(connection, listener);
        }

        @Override
        public boolean removeListener() {
            return connection.removeStanzaListener(listener);
        }
    }

    static class AsyncStanzaListenerHandle extends ListenerHandle {
        AsyncStanzaListenerHandle(XMPPConnection connection, StanzaListener listener) {
            super(connection, listener);
        }

        @Override
        public boolean removeListener() {
            return connection.removeAsyncStanzaListener(listener);
        }
    }

    static class SyncStanzaListenerHandle extends ListenerHandle {
        SyncStanzaListenerHandle(XMPPConnection connection, StanzaListener listener) {
            super(connection, listener);
        }

        @Override
        public boolean removeListener() {
            return connection.removeSyncStanzaListener(listener);
        }
    }
}
