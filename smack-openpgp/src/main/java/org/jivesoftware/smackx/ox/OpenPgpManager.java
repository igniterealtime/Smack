/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

import org.jxmpp.jid.BareJid;

public final class OpenPgpManager extends Manager {

    public static final String PEP_NODE_PUBLIC_KEYS = "urn:xmpp:openpgp:0:public-keys";

    public static String PEP_NODE_PUBLIC_KEY(String id) {
        return PEP_NODE_PUBLIC_KEYS + ":" + id;
    }

    private static final Map<XMPPConnection, OpenPgpManager> INSTANCES = new WeakHashMap<>();

    private OpenPgpManager(XMPPConnection connection) {
        super(connection);
    }

    public static OpenPgpManager getInstanceFor(XMPPConnection connection) {
        OpenPgpManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OpenPgpManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public static String bareJidToIdentity(BareJid jid) {
        return "xmpp:" + jid.toString();
    }

    public static OpenPgpMessage toOpenPgpMessage(InputStream is) {
        return null;
    }

    public void publishPublicKey() {
        // Check if key available at data node
        // If not, publish key to data node
        // Publish ID to metadata node
    }
}
