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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.provider.JingleIBBTransportProvider;

/**
 * Created by vanitas on 21.07.17.
 */
public final class JingleIBBTransportManager extends Manager implements JingleTransportManager {

    private static final WeakHashMap<XMPPConnection, JingleIBBTransportManager> INSTANCES = new WeakHashMap<>();

    static {
        JingleManager.addJingleTransportAdapter(new JingleIBBTransportAdapter());
        JingleManager.addJingleTransportProvider(new JingleIBBTransportProvider());
    }

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.addJingleTransportManager(this);
    }

    public static JingleIBBTransportManager getInstanceFor(XMPPConnection connection) {
        JingleIBBTransportManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleIBBTransportManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }

    @Override
    public JingleTransport<?> createTransportForInitiator(JingleContent content) {
        return new JingleIBBTransport();
    }

    @Override
    public JingleTransport<?> createTransportForResponder(JingleContent content, JingleTransport<?> peersTransport) {
        JingleIBBTransport other = (JingleIBBTransport) peersTransport;
        return new JingleIBBTransport(other.getStreamId(), (short) Math.min(other.getBlockSize(), JingleIBBTransport.MAX_BLOCKSIZE));
    }

    @Override
    public JingleTransport<?> createTransportForResponder(JingleContent content, JingleContentTransportElement peersTransportElement) {
        JingleIBBTransport other = new JingleIBBTransportAdapter().transportFromElement(peersTransportElement);
        return createTransportForResponder(content, other);
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public int compareTo(JingleTransportManager other) {
        return getPriority() > other.getPriority() ? 1 : -1;
    }
}
