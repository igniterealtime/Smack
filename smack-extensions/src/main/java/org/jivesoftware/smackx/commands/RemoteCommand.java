/**
 *
 * Copyright 2005-2007 Jive Software.
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
package org.jivesoftware.smackx.commands;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
 * Represents a command that is in a remote location. Invoking one of the
 * {@link AdHocCommand.Action#execute execute}, {@link AdHocCommand.Action#next next},
 * {@link AdHocCommand.Action#prev prev}, {@link AdHocCommand.Action#cancel cancel} or
 * {@link AdHocCommand.Action#complete complete} actions results in executing that
 * action in the remote location. In response to that action the internal state
 * of the this command instance will change. For example, if the command is a
 * single stage command, then invoking the execute action will execute this
 * action in the remote location. After that the local instance will have a
 * state of "completed" and a form or notes that applies.
 *
 * @author Gabriel Guardincerri
 *
 */
public class RemoteCommand extends AdHocCommand {

    /**
     * The connection that is used to execute this command
     */
    private final XMPPConnection connection;

    /**
     * The full JID of the command host
     */
    private final Jid jid;

    /**
     * The session ID of this execution.
     */
    private String sessionID;

    /**
     * Creates a new RemoteCommand that uses an specific connection to execute a
     * command identified by <code>node</code> in the host identified by
     * <code>jid</code>
     *
     * @param connection the connection to use for the execution.
     * @param node the identifier of the command.
     * @param jid the JID of the host.
     */
    protected RemoteCommand(XMPPConnection connection, String node, Jid jid) {
        super();
        this.connection = connection;
        this.jid = jid;
        this.setNode(node);
    }

    @Override
    public void cancel() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.cancel);
    }

    @Override
    public void complete(FillableForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.complete, form.getDataFormToSubmit());
    }

    @Override
    public void execute() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.execute);
    }

    /**
     * Executes the default action of the command with the information provided
     * in the Form. This form must be the answer form of the previous stage. If
     * there is a problem executing the command it throws an XMPPException.
     *
     * @param form the form answer of the previous stage.
     * @throws XMPPErrorException if an error occurs.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void execute(FillableForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.execute, form.getDataFormToSubmit());
    }

    @Override
    public void next(FillableForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.next, form.getDataFormToSubmit());
    }

    @Override
    public void prev() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(Action.prev);
    }

    private void executeAction(Action action) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(action, null);
    }

    /**
     * Executes the <code>action</code> with the <code>form</code>.
     * The action could be any of the available actions. The form must
     * be the answer of the previous stage. It can be <code>null</code> if it is the first stage.
     *
     * @param action the action to execute.
     * @param form the form with the information.
     * @throws XMPPErrorException if there is a problem executing the command.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private void executeAction(Action action, DataForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // TODO: Check that all the required fields of the form were filled, if
        // TODO: not throw the corresponding exception. This will make a faster response,
        // TODO: since the request is stopped before it's sent.
        AdHocCommandData data = new AdHocCommandData();
        data.setType(IQ.Type.set);
        data.setTo(getOwnerJID());
        data.setNode(getNode());
        data.setSessionID(sessionID);
        data.setAction(action);
        data.setForm(form);

        AdHocCommandData responseData = null;
        try {
            responseData = connection.sendIqRequestAndWaitForResponse(data);
        }
        finally {
            // We set the response data in a 'finally' block, so that it also gets set even if an error IQ was returned.
            if (responseData != null) {
                this.sessionID = responseData.getSessionID();
                super.setData(responseData);
            }
        }

    }

    @Override
    public Jid getOwnerJID() {
        return jid;
    }
}
