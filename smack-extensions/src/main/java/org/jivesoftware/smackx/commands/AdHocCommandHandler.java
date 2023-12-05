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
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder;
import org.jivesoftware.smackx.xdata.form.SubmitForm;

import org.jxmpp.jid.Jid;

/**
 * Represents a command that can be executed locally from a remote location. This
 * class must be extended to implement an specific ad-hoc command. This class
 * provides some useful tools:<ul>
 *      <li>Node</li>
 *      <li>Name</li>
 *      <li>Session ID</li>
 *      <li>Current Stage</li>
 *      <li>Available actions</li>
 *      <li>Default action</li>
 * </ul>
 * To implement a new command extend this class and implement all the abstract
 * methods. When implementing the actions remember that they could be invoked
 * several times, and that you must use the current stage number to know what to
 * do.
 *
 * @author Gabriel Guardincerri
 * @author Florian Schmaus
 */
public abstract class AdHocCommandHandler extends AbstractAdHocCommand {

    /**
     * The time stamp of first invocation of the command. Used to implement the session timeout.
     */
    private final long creationDate;

    /**
     * The number of the current stage.
     */
    private int currentStage;

    public AdHocCommandHandler(String node, String name, String sessionId) {
        super(node, name);
        setSessionId(sessionId);
        this.creationDate = System.currentTimeMillis();
    }

    protected abstract AdHocCommandData execute(AdHocCommandDataBuilder response) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, IllegalStateException;

    protected abstract AdHocCommandData next(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
                    IllegalStateException;

    protected abstract AdHocCommandData complete(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
                    IllegalStateException;

    protected abstract AdHocCommandData prev(AdHocCommandDataBuilder response) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, IllegalStateException;

    /**
     * Returns the date the command was created.
     *
     * @return the date the command was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Returns true if the specified requester has permission to execute all the
     * stages of this action. This is checked when the first request is received,
     * if the permission is grant then the requester will be able to execute
     * all the stages of the command. It is not checked again during the
     * execution.
     *
     * @param jid the JID to check permissions on.
     * @return true if the user has permission to execute this action.
     */
    public boolean hasPermission(Jid jid) {
        return true;
    };

    /**
     * Returns the currently executing stage number. The first stage number is
     * 1. During the execution of the first action this method will answer 1.
     *
     * @return the current stage number.
     */
    public final int getCurrentStage() {
        return currentStage;
    }

    /**
     * Increase the current stage number.
     */
    final void incrementStage() {
        currentStage++;
    }

    /**
     * Decrease the current stage number.
     */
    final void decrementStage() {
        currentStage--;
    }

    protected static XMPPErrorException newXmppErrorException(StanzaError.Condition condition) {
        return newXmppErrorException(condition, null);
    }

    protected static XMPPErrorException newXmppErrorException(StanzaError.Condition condition, String descriptiveText) {
        StanzaError stanzaError = StanzaError.from(condition, descriptiveText).build();
        return new XMPPErrorException(null, stanzaError);
    }

    protected static XMPPErrorException newBadRequestException(String descriptiveTest) {
        return newXmppErrorException(StanzaError.Condition.bad_request, descriptiveTest);
    }

    public abstract static class SingleStage extends AdHocCommandHandler {

        public SingleStage(String node, String name, String sessionId) {
            super(node, name, sessionId);
        }

        protected abstract AdHocCommandData executeSingleStage(AdHocCommandDataBuilder response)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException;

        @Override
        protected final AdHocCommandData execute(AdHocCommandDataBuilder response)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
            response.setStatusCompleted();
            return executeSingleStage(response);
        }

        @Override
        public final AdHocCommandData next(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
            throw newXmppErrorException(StanzaError.Condition.bad_request);
        }

        @Override
        public final AdHocCommandData complete(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
            throw newXmppErrorException(StanzaError.Condition.bad_request);
        }

        @Override
        public final AdHocCommandData prev(AdHocCommandDataBuilder response)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
            throw newXmppErrorException(StanzaError.Condition.bad_request);
        }

        @Override
        public final void cancel()
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
            throw newXmppErrorException(StanzaError.Condition.bad_request);
        }

    }
}
