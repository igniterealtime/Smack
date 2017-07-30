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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Created by vanitas on 20.06.17.
 */
public abstract class JingleTransportSession<T extends JingleContentTransport> {
    protected final JingleSession jingleSession;
    protected T ourProposal, theirProposal;

    public JingleTransportSession(JingleSession session) {
        this.jingleSession = session;
    }

    public abstract T createTransport();

    public void processJingle(Jingle jingle) {
        if (jingle.getContents().size() == 0) {
            return;
        }

        JingleContent content = jingle.getContents().get(0);
        JingleContentTransport t = content.getTransport();

        if (t != null && t.getNamespace().equals(getNamespace())) {
            setTheirProposal(t);
        }
    }

    public abstract void setTheirProposal(JingleContentTransport transport);

    public abstract void initiateOutgoingSession(JingleTransportInitiationCallback callback);

    public abstract void initiateIncomingSession(JingleTransportInitiationCallback callback);

    public abstract String getNamespace();

    public abstract IQ handleTransportInfo(Jingle transportInfo);

    public abstract JingleTransportManager<T> transportManager();
}
