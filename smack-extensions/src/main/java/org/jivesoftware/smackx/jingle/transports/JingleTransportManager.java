/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleContentImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Manager for a JingleTransport method.
 * @param <D> JingleContentTransport.
 */
public abstract class JingleTransportManager<D extends JingleContentTransport> implements ConnectionListener {

    private final XMPPConnection connection;

    public JingleTransportManager(XMPPConnection connection) {
        this.connection = connection;
        connection.addConnectionListener(this);
    }

    public XMPPConnection getConnection() {
        return connection();
    }

    public XMPPConnection connection() {
        return connection;
    }

    public abstract String getNamespace();

    public abstract JingleTransportSession<D> transportSession(JingleSession jingleSession);

    @Override
    public void connected(XMPPConnection connection) {
    }

    @Override
    public void connectionClosed() {
    }

    @Override
    public void connectionClosedOnError(Exception e) {
    }

    /**
     * Return a (usually) positive integer, which is used to define a strict order over the set of available transport managers.
     * @return priority.
     */
    public abstract int getPriority();

    public int compareTo(JingleTransportManager<?> other) {
        return getPriority() > other.getPriority() ? 1 : -1;
    }

    public abstract JingleTransport<?> createTransportForInitiator(JingleContentImpl content);

    // JingleTransport<?> createTransportForResponder(JingleContentImpl content, JingleTransport<?> peersTransport);

    public abstract JingleTransport<?> createTransportForResponder(JingleContentImpl content, JingleContentTransport peersTransportElement);

}
