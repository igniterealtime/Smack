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
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
 * Represents a ad-hoc command invoked on a remote entity. Invoking one of the
 * {@link #execute()}, {@link #next(SubmitForm)},
 * {@link #prev()}, {@link #cancel()} or
 * {@link #complete(SubmitForm)} actions results in executing that
 * action on the remote entity. In response to that action the internal state
 * of the this command instance will change. For example, if the command is a
 * single stage command, then invoking the execute action will execute this
 * action in the remote location. After that the local instance will have a
 * state of "completed" and a form or notes that applies.
 *
 * @author Gabriel Guardincerri
 * @author Florian Schmaus
 *
 */
public class AdHocCommand extends AbstractAdHocCommand {

    /**
     * The connection that is used to execute this command
     */
    private final XMPPConnection connection;

    /**
     * The full JID of the command host
     */
    private final Jid jid;

    /**
     * Creates a new RemoteCommand that uses an specific connection to execute a
     * command identified by <code>node</code> in the host identified by
     * <code>jid</code>
     *
     * @param connection the connection to use for the execution.
     * @param node the identifier of the command.
     * @param jid the JID of the host.
     */
    protected AdHocCommand(XMPPConnection connection, String node, Jid jid) {
        super(node);
        this.connection = Objects.requireNonNull(connection);
        this.jid = Objects.requireNonNull(jid);
    }

    public Jid getOwnerJID() {
        return jid;
    }

    @Override
    public final void cancel() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        executeAction(AdHocCommandData.Action.cancel);
    }

    /**
     * Executes the command. This is invoked only on the first stage of the
     * command. It is invoked on every command. If there is a problem executing
     * the command it throws an XMPPException.
     *
     * @return an ad-hoc command result.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there is an error executing the command.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public final AdHocCommandResult execute() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(AdHocCommandData.Action.execute);
    }

    /**
     * Executes the next action of the command with the information provided in
     * the <code>response</code>. This form must be the answer form of the
     * previous stage. This method will be only invoked for commands that have one
     * or more stages. If there is a problem executing the command it throws an
     * XMPPException.
     *
     * @param filledForm the form answer of the previous stage.
     * @return an ad-hoc command result.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there is a problem executing the command.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public final AdHocCommandResult next(SubmitForm filledForm) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(AdHocCommandData.Action.next, filledForm.getDataForm());
    }

    /**
     * Completes the command execution with the information provided in the
     * <code>response</code>. This form must be the answer form of the
     * previous stage. This method will be only invoked for commands that have one
     * or more stages. If there is a problem executing the command it throws an
     * XMPPException.
     *
     * @param filledForm the form answer of the previous stage.
     * @return an ad-hoc command result.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there is a problem executing the command.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public AdHocCommandResult complete(SubmitForm filledForm) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(AdHocCommandData.Action.complete, filledForm.getDataForm());
    }

    /**
     * Goes to the previous stage. The requester is asking to re-send the
     * information of the previous stage. The command must change it state to
     * the previous one. If there is a problem executing the command it throws
     * an XMPPException.
     *
     * @return an ad-hoc command result.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there is a problem executing the command.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public final AdHocCommandResult prev() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(AdHocCommandData.Action.prev);
    }

    /**
     * Executes the default action of the command with the information provided
     * in the Form. This form must be the answer form of the previous stage. If
     * there is a problem executing the command it throws an XMPPException.
     *
     * @param form the form answer of the previous stage.
     * @return an ad-hoc command result.
     * @throws XMPPErrorException if an error occurs.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public final AdHocCommandResult execute(FillableForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(AdHocCommandData.Action.execute, form.getDataFormToSubmit());
    }

    private AdHocCommandResult executeAction(AdHocCommandData.Action action) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return executeAction(action, null);
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
    private synchronized AdHocCommandResult executeAction(AdHocCommandData.Action action, DataForm form) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AdHocCommandData request = AdHocCommandData.builder(getNode(), connection)
                        .ofType(IQ.Type.set)
                        .to(getOwnerJID())
                        .setSessionId(getSessionId())
                        .setAction(action)
                        .setForm(form)
                        .build();

        addRequest(request);

        AdHocCommandData response = connection.sendIqRequestAndWaitForResponse(request);

        // The Ad-Hoc service ("server") may have generated a session id for us.
        String sessionId = response.getSessionId();
        if (sessionId != null) {
            setSessionId(sessionId);
        }

        AdHocCommandResult result = AdHocCommandResult.from(response);
        addResult(result);
        return result;
    }

}
