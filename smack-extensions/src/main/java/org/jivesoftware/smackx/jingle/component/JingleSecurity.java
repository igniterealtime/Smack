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
package org.jivesoftware.smackx.jingle.component;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.callback.JingleSecurityCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

import org.jxmpp.jid.FullJid;

/**
 * Class that represents a contents security component.
 */
public abstract class JingleSecurity<D extends JingleContentSecurityElement> {

    /**
     * Parent of this security component.
     */
    private JingleContent parent;

    /**
     * Return a {@link JingleContentSecurityElement} that represents this {@link JingleSecurity} component.
     * @return element.
     */
    public abstract D getElement();

    /**
     * Handle an incoming security-info.
     * @param element security info.
     * @param wrapping jingleElement that contains the security info.
     * @return result.
     */
    public abstract JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element, JingleElement wrapping);

    /**
     * Set the parent {@link JingleContent} of this security component.
     * @param parent parent.
     */
    public void setParent(JingleContent parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    /**
     * Return the parent {@link JingleContent} of this security component.
     * @return parent.
     */
    public JingleContent getParent() {
        return parent;
    }

    /**
     * Decrypt an incoming bytestream.
     * This includes wrapping the incoming {@link BytestreamSession} in a {@link JingleSecurityBytestreamSession} and
     * pass it to the callbacks {@link JingleSecurityCallback#onSecurityReady(BytestreamSession)} method.
     * @param bytestreamSession encrypted bytestreamSession.
     * @param callback callback.
     */
    public abstract void decryptIncomingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback);

    /**
     * Encrypt an incoming bytestream.
     * This includes wrapping the incoming {@link BytestreamSession} in a {@link JingleSecurityBytestreamSession} and
     * pass it to the callbacks {@link JingleSecurityCallback#onSecurityReady(BytestreamSession)} method.
     * @param bytestreamSession encrypted bytestreamSession.
     * @param callback callback.
     */
    public abstract void encryptOutgoingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback);

    /**
     * Return the namespace of this security component.
     * @return namespace.
     */
    public abstract String getNamespace();

    /**
     * Prepare the security session.
     * @param connection connection.
     * @param peer peer.
     */
    public abstract void prepare(XMPPConnection connection, FullJid peer);
}
