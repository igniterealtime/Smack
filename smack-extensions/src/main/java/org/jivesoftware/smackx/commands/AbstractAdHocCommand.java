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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Action;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.AllowedAction;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Status;

/**
 * An ad-hoc command is responsible for executing the provided service and
 * storing the result of the execution. Each new request will create a new
 * instance of the command, allowing information related to executions to be
 * stored in it. For example suppose that a command that retrieves the list of
 * users on a server is implemented. When the command is executed it gets that
 * list and the result is stored as a form in the command instance, i.e. the
 * <code>getForm</code> method retrieves a form with all the users.
 * <p>
 * Each command has a <code>node</code> that should be unique within a given JID.
 * </p>
 * <p>
 * Commands may have zero or more stages. Each stage is usually used for
 * gathering information required for the command execution. Users are able to
 * move forward or backward across the different stages. Commands may not be
 * cancelled while they are being executed. However, users may request the
 * "cancel" action when submitting a stage response indicating that the command
 * execution should be aborted. Thus, releasing any collected information.
 * Commands that require user interaction (i.e. have more than one stage) will
 * have to provide the data forms the user must complete in each stage and the
 * allowed actions the user might perform during each stage (e.g. go to the
 * previous stage or go to the next stage).
 * </p>
 * All the actions may throw an XMPPException if there is a problem executing
 * them. The <code>XMPPError</code> of that exception may have some specific
 * information about the problem. The possible extensions are:
 * <ul>
 * <li><i>malformed-action</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-action</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-locale</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-payload</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-sessionid</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>session-expired</i>. Extension of a <i>not-allowed</i> error.</li>
 * </ul>
 * <p>
 * See the <code>SpecificErrorCondition</code> class for detailed description
 * of each one.
 * </p>
 * Use the <code>getSpecificErrorConditionFrom</code> to obtain the specific
 * information from an <code>XMPPError</code>.
 *
 * @author Gabriel Guardincerri
 * @author Florian Schmaus
 *
 */
public abstract class AbstractAdHocCommand {
    private final List<AdHocCommandData> requests = new ArrayList<>();
    private final List<AdHocCommandResult> results = new ArrayList<>();

    private final String node;

    private final String name;

    /**
     * The session ID of this execution.
     */
    private String sessionId;

    protected AbstractAdHocCommand(String node, String name) {
        this.node = StringUtils.requireNotNullNorEmpty(node, "Ad-Hoc command node must be given");
        this.name = name;
    }

    protected AbstractAdHocCommand(String node) {
        this(node, null);
    }

    void addRequest(AdHocCommandData request) {
        requests.add(request);
    }

    void addResult(AdHocCommandResult result) {
        results.add(result);
    }

    /**
     * Returns the specific condition of the <code>error</code> or <code>null</code> if the
     * error doesn't have any.
     *
     * @param error the error the get the specific condition from.
     * @return the specific condition of this error, or null if it doesn't have
     *         any.
     */
    public static SpecificErrorCondition getSpecificErrorCondition(StanzaError error) {
        // This method is implemented to provide an easy way of getting a packet
        // extension of the XMPPError.
        for (SpecificErrorCondition condition : SpecificErrorCondition.values()) {
            if (error.getExtension(condition.toString(),
                    AdHocCommandData.SpecificError.namespace) != null) {
                return condition;
            }
        }
        return null;
    }

    /**
     * Returns the human readable name of the command.
     *
     * @return the human readable name of the command
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unique identifier of the command. It is unique for the
     * <code>OwnerJID</code>.
     *
     * @return the unique identifier of the command.
     */
    public String getNode() {
        return node;
    }

    public String getSessionId() {
        return sessionId;
    }

    protected void setSessionId(String sessionId) {
        assert this.sessionId == null || this.sessionId.equals(sessionId);
        this.sessionId = StringUtils.requireNotNullNorEmpty(sessionId, "Must provide a session ID");
    }

    public AdHocCommandData getLastRequest() {
        if (requests.isEmpty()) return null;
        return requests.get(requests.size() - 1);
    }

    public AdHocCommandResult getLastResult() {
        if (results.isEmpty()) return null;
        return results.get(results.size() - 1);
    }

    /**
     * Returns the notes that the command has at the current stage.
     *
     * @return a list of notes.
     */
    public List<AdHocCommandNote> getNotes() {
        AdHocCommandResult result = getLastResult();
        if (result == null) return null;

        return result.getResponse().getNotes();
    }

    /**
     * Cancels the execution of the command. This can be invoked on any stage of
     * the execution. If there is a problem executing the command it throws an
     * XMPPException.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there is a problem executing the command.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public abstract void cancel() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException;

    /**
     * Returns a collection with the allowed actions based on the current stage.
     * Possible actions are: {@link AllowedAction#prev prev}, {@link AllowedAction#next next} and
     * {@link AllowedAction#complete complete}. This method will be only invoked for commands that
     * have one or more stages.
     *
     * @return a collection with the allowed actions based on the current stage
     *      as defined in the SessionData.
     */
    public final Set<AllowedAction> getActions() {
        AdHocCommandResult result = getLastResult();
        if (result == null) return null;

        return result.getResponse().getActions();
    }

    /**
     * Returns the action available for the current stage which is
     * considered the equivalent to "execute". When the requester sends his
     * reply, if no action was defined in the command then the action will be
     * assumed "execute" thus assuming the action returned by this method. This
     * method will never be invoked for commands that have no stages.
     *
     * @return the action available for the current stage which is considered
     *      the equivalent to "execute".
     */
    protected AllowedAction getExecuteAction() {
        AdHocCommandResult result = getLastResult();
        if (result == null) return null;

        return result.getResponse().getExecuteAction();
    }

    /**
     * Returns the status of the current stage.
     *
     * @return the current status.
     */
    public Status getStatus() {
        AdHocCommandResult result = getLastResult();
        if (result == null) return null;

        return result.getResponse().getStatus();
    }

    /**
     * Check if this command has been completed successfully.
     *
     * @return <code>true</code> if this command is completed.
     * @since 4.2
     */
    public boolean isCompleted() {
        return getStatus() == AdHocCommandData.Status.completed;
    }

    /**
     * Returns true if the <code>action</code> is available in the current stage.
     * The {@link Action#cancel cancel} action is always allowed. To define the
     * available actions use the <code>addActionAvailable</code> method.
     *
     * @param action The action to check if it is available.
     * @return True if the action is available for the current stage.
     */
    public final boolean isValidAction(Action action) {
        if (action == Action.cancel) {
            return true;
        }

        final AllowedAction executeAction;
        if (action == Action.execute) {
            AdHocCommandResult result = getLastResult();
            executeAction = result.getResponse().getExecuteAction();

            // This is basically the case that was clarified with
            // https://github.com/xsf/xeps/commit/fdaee2da8ffd34b5b5151e90ef1df8b396a06531 and
            // https://github.com/xsf/xeps/pull/591.
            if (executeAction == null) {
                return false;
            }
        } else {
            executeAction = action.allowedAction;
            assert executeAction != null;
        }

        Set<AllowedAction> actions = getActions();
        return actions != null && actions.contains(executeAction);
    }
}
