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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.AdHocCommandData;

import java.util.List;

/**
 * An ad-hoc command is responsible for executing the provided service and
 * storing the result of the execution. Each new request will create a new
 * instance of the command, allowing information related to executions to be
 * stored in it. For example suppose that a command that retrieves the list of
 * users on a server is implemented. When the command is executed it gets that
 * list and the result is stored as a form in the command instance, i.e. the
 * <code>getForm</code> method retrieves a form with all the users.
 * <p>
 * Each command has a <tt>node</tt> that should be unique within a given JID.
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
 * <p>
 * All the actions may throw an XMPPException if there is a problem executing
 * them. The <code>XMPPError</code> of that exception may have some specific
 * information about the problem. The possible extensions are:
 * 
 * <li><i>malformed-action</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-action</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-locale</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-payload</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>bad-sessionid</i>. Extension of a <i>bad-request</i> error.</li>
 * <li><i>session-expired</i>. Extension of a <i>not-allowed</i> error.</li>
 * <p>
 * See the <code>SpecificErrorCondition</code> class for detailed description
 * of each one.
 * <p>
 * Use the <code>getSpecificErrorConditionFrom</code> to obtain the specific
 * information from an <code>XMPPError</code>.
 * 
 * @author Gabriel Guardincerri
 * 
 */
public abstract class AdHocCommand {
    // TODO: Analyze the redesign of command by having an ExecutionResponse as a
    // TODO: result to the execution of every action. That result should have all the
    // TODO: information related to the execution, e.g. the form to fill. Maybe this
    // TODO: design is more intuitive and simpler than the current one that has all in
    // TODO: one class.

    private AdHocCommandData data;

    public AdHocCommand() {
        super();
        data = new AdHocCommandData();
    }

    /**
     * Returns the specific condition of the <code>error</code> or <tt>null</tt> if the
     * error doesn't have any.
     * 
     * @param error the error the get the specific condition from.
     * @return the specific condition of this error, or null if it doesn't have
     *         any.
     */
    public static SpecificErrorCondition getSpecificErrorCondition(XMPPError error) {
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
     * Set the the human readable name of the command, usually used for
     * displaying in a UI.
     * 
     * @param name the name.
     */
    public void setName(String name) {
        data.setName(name);
    }

    /**
     * Returns the human readable name of the command.
     * 
     * @return the human readable name of the command
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Sets the unique identifier of the command. This value must be unique for
     * the <code>OwnerJID</code>.
     * 
     * @param node the unique identifier of the command.
     */
    public void setNode(String node) {
        data.setNode(node);
    }

    /**
     * Returns the unique identifier of the command. It is unique for the
     * <code>OwnerJID</code>.
     * 
     * @return the unique identifier of the command.
     */
    public String getNode() {
        return data.getNode();
    }

    /**
     * Returns the full JID of the owner of this command. This JID is the "to" of a
     * execution request.
     * 
     * @return the owner JID.
     */
    public abstract String getOwnerJID();

    /**
     * Returns the notes that the command has at the current stage.
     * 
     * @return a list of notes.
     */
    public List<AdHocCommandNote> getNotes() {
        return data.getNotes();
    }

    /**
     * Adds a note to the current stage. This should be used when setting a
     * response to the execution of an action. All the notes added here are
     * returned by the {@link #getNotes} method during the current stage.
     * Once the stage changes all the notes are discarded.
     * 
     * @param note the note.
     */
    protected void addNote(AdHocCommandNote note) {
        data.addNote(note);
    }

    public String getRaw() {
        return data.getChildElementXML();
    }

    /**
     * Returns the form of the current stage. Usually it is the form that must
     * be answered to execute the next action. If that is the case it should be
     * used by the requester to fill all the information that the executor needs
     * to continue to the next stage. It can also be the result of the
     * execution.
     * 
     * @return the form of the current stage to fill out or the result of the
     *         execution.
     */
    public Form getForm() {
        if (data.getForm() == null) {
            return null;
        }
        else {
            return new Form(data.getForm());
        }
    }

    /**
     * Sets the form of the current stage. This should be used when setting a
     * response. It could be a form to fill out the information needed to go to
     * the next stage or the result of an execution.
     * 
     * @param form the form of the current stage to fill out or the result of the
     *      execution.
     */
    protected void setForm(Form form) {
        data.setForm(form.getDataFormToSend());
    }

    /**
     * Executes the command. This is invoked only on the first stage of the
     * command. It is invoked on every command. If there is a problem executing
     * the command it throws an XMPPException.
     * 
     * @throws XMPPException if there is an error executing the command.
     */
    public abstract void execute() throws XMPPException;

    /**
     * Executes the next action of the command with the information provided in
     * the <code>response</code>. This form must be the answer form of the
     * previous stage. This method will be only invoked for commands that have one
     * or more stages. If there is a problem executing the command it throws an
     * XMPPException.
     * 
     * @param response the form answer of the previous stage.
     * @throws XMPPException if there is a problem executing the command.
     */
    public abstract void next(Form response) throws XMPPException;

    /**
     * Completes the command execution with the information provided in the
     * <code>response</code>. This form must be the answer form of the
     * previous stage. This method will be only invoked for commands that have one
     * or more stages. If there is a problem executing the command it throws an
     * XMPPException.
     * 
     * @param response the form answer of the previous stage.
     * @throws XMPPException if there is a problem executing the command.
     */
    public abstract void complete(Form response) throws XMPPException;

    /**
     * Goes to the previous stage. The requester is asking to re-send the
     * information of the previous stage. The command must change it state to
     * the previous one. If there is a problem executing the command it throws
     * an XMPPException.
     * 
     * @throws XMPPException if there is a problem executing the command.
     */
    public abstract void prev() throws XMPPException;

    /**
     * Cancels the execution of the command. This can be invoked on any stage of
     * the execution. If there is a problem executing the command it throws an
     * XMPPException.
     * 
     * @throws XMPPException if there is a problem executing the command.
     */
    public abstract void cancel() throws XMPPException;

    /**
     * Returns a collection with the allowed actions based on the current stage.
     * Possible actions are: {@link Action#prev prev}, {@link Action#next next} and
     * {@link Action#complete complete}. This method will be only invoked for commands that
     * have one or more stages.
     * 
     * @return a collection with the allowed actions based on the current stage
     *      as defined in the SessionData.
     */
    protected List<Action> getActions() {
        return data.getActions();
    }

    /**
     * Add an action to the current stage available actions. This should be used
     * when creating a response.
     * 
     * @param action the action.
     */
    protected void addActionAvailable(Action action) {
        data.addAction(action);
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
    protected Action getExecuteAction() {
        return data.getExecuteAction();
    }

    /**
     * Sets which of the actions available for the current stage is
     * considered the equivalent to "execute". This should be used when setting
     * a response. When the requester sends his reply, if no action was defined
     * in the command then the action will be assumed "execute" thus assuming
     * the action returned by this method.
     * 
     * @param action the action.
     */
    protected void setExecuteAction(Action action) {
        data.setExecuteAction(action);
    }

    /**
     * Returns the status of the current stage.
     * 
     * @return the current status.
     */
    public Status getStatus() {
        return data.getStatus();
    }

    /**
     * Sets the data of the current stage. This should not used.
     * 
     * @param data the data.
     */
    void setData(AdHocCommandData data) {
        this.data = data;
    }

    /**
     * Gets the data of the current stage. This should not used.
     *
     * @return the data.
     */
    AdHocCommandData getData() {
        return data;
    }

    /**
     * Returns true if the <code>action</code> is available in the current stage.
     * The {@link Action#cancel cancel} action is always allowed. To define the
     * available actions use the <code>addActionAvailable</code> method.
     * 
     * @param action
     *            The action to check if it is available.
     * @return True if the action is available for the current stage.
     */
    protected boolean isValidAction(Action action) {
        return getActions().contains(action) || Action.cancel.equals(action);
    }

    /**
     * The status of the stage in the adhoc command.
     */
    public enum Status {

        /**
         * The command is being executed.
         */
        executing,

        /**
         * The command has completed. The command session has ended.
         */
        completed,

        /**
         * The command has been canceled. The command session has ended.
         */
        canceled
    }

    public enum Action {

        /**
         * The command should be executed or continue to be executed. This is
         * the default value.
         */
        execute,

        /**
         * The command should be canceled.
         */
        cancel,

        /**
         * The command should be digress to the previous stage of execution.
         */
        prev,

        /**
         * The command should progress to the next stage of execution.
         */
        next,

        /**
         * The command should be completed (if possible).
         */
        complete,

        /**
         * The action is unknow. This is used when a recieved message has an
         * unknown action. It must not be used to send an execution request.
         */
        unknown
    }

    public enum SpecificErrorCondition {

        /**
         * The responding JID cannot accept the specified action.
         */
        badAction("bad-action"),

        /**
         * The responding JID does not understand the specified action.
         */
        malformedAction("malformed-action"),

        /**
         * The responding JID cannot accept the specified language/locale.
         */
        badLocale("bad-locale"),

        /**
         * The responding JID cannot accept the specified payload (e.g. the data
         * form did not provide one or more required fields).
         */
        badPayload("bad-payload"),

        /**
         * The responding JID cannot accept the specified sessionid.
         */
        badSessionid("bad-sessionid"),

        /**
         * The requesting JID specified a sessionid that is no longer active
         * (either because it was completed, canceled, or timed out).
         */
        sessionExpired("session-expired");

        private String value;

        SpecificErrorCondition(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}