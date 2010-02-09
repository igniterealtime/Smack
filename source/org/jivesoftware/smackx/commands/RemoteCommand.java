/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2005-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.AdHocCommandData;

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
    private Connection connection;

    /**
     * The full JID of the command host
     */
    private String jid;

    /**
     * The session ID of this execution.
     */
    private String sessionID;


    /**
     * The number of milliseconds to wait for a response from the server
     * The default value is the default packet reply timeout (5000 ms).
     */
    private long packetReplyTimeout;

    /**
     * Creates a new RemoteCommand that uses an specific connection to execute a
     * command identified by <code>node</code> in the host identified by
     * <code>jid</code>
     *
     * @param connection the connection to use for the execution.
     * @param node the identifier of the command.
     * @param jid the JID of the host.
     */
    protected RemoteCommand(Connection connection, String node, String jid) {
        super();
        this.connection = connection;
        this.jid = jid;
        this.setNode(node);
        this.packetReplyTimeout = SmackConfiguration.getPacketReplyTimeout();
    }

    @Override
    public void cancel() throws XMPPException {
        executeAction(Action.cancel, packetReplyTimeout);
    }

    @Override
    public void complete(Form form) throws XMPPException {
        executeAction(Action.complete, form, packetReplyTimeout);
    }

    @Override
    public void execute() throws XMPPException {
        executeAction(Action.execute, packetReplyTimeout);
    }

    /**
     * Executes the default action of the command with the information provided
     * in the Form. This form must be the anwser form of the previous stage. If
     * there is a problem executing the command it throws an XMPPException.
     *
     * @param form the form anwser of the previous stage.
     * @throws XMPPException if an error occurs.
     */
    public void execute(Form form) throws XMPPException {
        executeAction(Action.execute, form, packetReplyTimeout);
    }

    @Override
    public void next(Form form) throws XMPPException {
        executeAction(Action.next, form, packetReplyTimeout);
    }

    @Override
    public void prev() throws XMPPException {
        executeAction(Action.prev, packetReplyTimeout);
    }

    private void executeAction(Action action, long packetReplyTimeout) throws XMPPException {
        executeAction(action, null, packetReplyTimeout);
    }

    /**
     * Executes the <code>action</codo> with the <code>form</code>.
     * The action could be any of the available actions. The form must
     * be the anwser of the previous stage. It can be <tt>null</tt> if it is the first stage.
     *
     * @param action the action to execute.
     * @param form the form with the information.
     * @param timeout the amount of time to wait for a reply.
     * @throws XMPPException if there is a problem executing the command.
     */
    private void executeAction(Action action, Form form, long timeout) throws XMPPException {
        // TODO: Check that all the required fields of the form were filled, if
        // TODO: not throw the corresponding exeption. This will make a faster response,
        // TODO: since the request is stoped before it's sent.
        AdHocCommandData data = new AdHocCommandData();
        data.setType(IQ.Type.SET);
        data.setTo(getOwnerJID());
        data.setNode(getNode());
        data.setSessionID(sessionID);
        data.setAction(action);

        if (form != null) {
            data.setForm(form.getDataFormToSend());
        }

        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(data.getPacketID()));

        connection.sendPacket(data);

        Packet response = collector.nextResult(timeout);

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }

        AdHocCommandData responseData = (AdHocCommandData) response;
        this.sessionID = responseData.getSessionID();
        super.setData(responseData);
    }

    @Override
    public String getOwnerJID() {
        return jid;
    }

    /**
     * Returns the number of milliseconds to wait for a respone. The
     * {@link SmackConfiguration#getPacketReplyTimeout default} value
     * should be adjusted for commands that can take a long time to execute.
     *
     * @return the number of milliseconds to wait for responses.
     */
    public long getPacketReplyTimeout() {
        return packetReplyTimeout;
    }

    /**
     * Returns the number of milliseconds to wait for a respone. The
     * {@link SmackConfiguration#getPacketReplyTimeout default} value
     * should be adjusted for commands that can take a long time to execute.
     *
     * @param packetReplyTimeout the number of milliseconds to wait for responses.
     */
    public void setPacketReplyTimeout(long packetReplyTimeout) {
        this.packetReplyTimeout = packetReplyTimeout;
    }
}