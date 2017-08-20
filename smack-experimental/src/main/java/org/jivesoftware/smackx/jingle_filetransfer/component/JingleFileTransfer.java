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
package org.jivesoftware.smackx.jingle_filetransfer.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.component.JingleDescription;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle_filetransfer.controller.JingleFileTransferController;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

/**
 * Created by vanitas on 22.07.17.
 */
public abstract class JingleFileTransfer extends JingleDescription<JingleFileTransferElement> implements JingleFileTransferController {

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";
    public static final String NAMESPACE = NAMESPACE_V5;

    protected State state;
    protected JingleFile metadata;

    private final List<ProgressListener> progressListeners = Collections.synchronizedList(new ArrayList<ProgressListener>());

    JingleFileTransfer(JingleFile metadata) {
        this.metadata = metadata;
    }

    public abstract boolean isOffer();

    public abstract boolean isRequest();

    @Override
    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
        //TODO: Notify new listener?
    }

    @Override
    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    @Override
    public void cancel(XMPPConnection connection) throws SmackException.NotConnectedException, InterruptedException {
        JingleSession session = getParent().getParent();
        switch (state) {
            case pending:
                if (session.isResponder()) {
                    connection.createStanzaCollectorAndSend(JingleElement.createSessionTerminate(session.getPeer(), session.getSessionId(), JingleReasonElement.Reason.decline));
                } else {
                    connection.createStanzaCollectorAndSend(JingleElement.createSessionTerminate(session.getPeer(), session.getSessionId(), JingleReasonElement.Reason.cancel));
                }
                break;

            case active:
                connection.createStanzaCollectorAndSend(JingleElement.createSessionTerminate(session.getPeer(), session.getSessionId(), JingleReasonElement.Reason.cancel));
                break;

            default: break;
        }
        getParent().onContentCancel();
    }

    public void notifyProgressListeners(float progress) {
        for (ProgressListener p : progressListeners) {
            p.progress(progress);
        }
    }

    public void notifyProgressListenersFinished() {
        for (ProgressListener p : progressListeners) {
            p.finished();
        }
    }

    public void notifyProgressListenersStarted() {
        for (ProgressListener p : progressListeners) {
            p.started();
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }

    @Override
    public JingleFileTransferElement getElement() {
        return new JingleFileTransferElement(metadata.getElement());
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public JingleFile getMetadata() {
        return metadata;
    }
}
