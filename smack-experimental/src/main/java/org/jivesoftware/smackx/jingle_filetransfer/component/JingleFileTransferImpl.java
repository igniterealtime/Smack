/**
 *
 * Copyright 2017-2022 Paul Schaub
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
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.component.JingleDescription;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_filetransfer.controller.JingleFileTransferController;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

/**
 * An abstract class implementation for JingleFileTransfer.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public abstract class JingleFileTransferImpl extends JingleDescription<JingleFileTransfer> implements JingleFileTransferController {

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";
    public static final String NAMESPACE = NAMESPACE_V5;

    protected State mState;
    protected JingleFile metadata;

    private final List<ProgressListener> progressListeners = Collections.synchronizedList(new ArrayList<>());

    JingleFileTransferImpl(JingleFile metadata) {
        this.metadata = metadata;
    }

    public abstract boolean isOffer();

    public abstract boolean isRequest();

    @Override
    public JingleSessionImpl getJingleSession() {
        return getParent().getParent();
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }

    @Override
    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    @Override
    public void cancel(XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        JingleSessionImpl session = getParent().getParent();
        JingleUtil jutil = new JingleUtil(connection);
        switch (mState) {
            case pending:
                if (session.isResponder()) {
                    jutil.sendSessionTerminateDecline(session.getRemote(), session.getSessionId());
                } else {
                    jutil.sendSessionTerminateCancel(session.getRemote(), session.getSessionId());
                }
                break;

            case active:
                jutil.sendSessionTerminateCancel(session.getRemote(), session.getSessionId());
                break;

            default: break;
        }
        getParent().onContentCancel();
    }

    public void notifyProgressListeners(int rwBytes) {
        for (ProgressListener p : progressListeners) {
            p.progress(rwBytes);
        }
    }

    public void notifyProgressListenersFinished() {
        for (ProgressListener p : progressListeners) {
            p.onFinished();
        }
    }

    public void notifyProgressListenersStarted() {
        for (ProgressListener p : progressListeners) {
            p.onStarted();
        }
    }

    public void notifyProgressListenersOnError(JingleReason.Reason reason, String error) {
        JingleReason jingleReason = new JingleReason(reason, error, null);

        for (ProgressListener p : progressListeners) {
            p.onError(jingleReason);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransferImpl.NAMESPACE;
    }

    @Override
    public JingleFileTransfer getElement() {
        return new JingleFileTransfer(Collections.singletonList(metadata.getElement()));
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public JingleFile getMetadata() {
        return metadata;
    }
}
