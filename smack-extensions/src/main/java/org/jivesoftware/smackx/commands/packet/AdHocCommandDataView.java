/**
 *
 * Copyright 2023 Florian Schmaus
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
package org.jivesoftware.smackx.commands.packet;

import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.IqView;

import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Action;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.AllowedAction;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Status;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public interface AdHocCommandDataView extends IqView {

    /**
     * Returns the identifier of the command.
     *
     * @return the node.
     */
    String getNode();

    /**
     * Returns the human name of the command.
     *
     * @return the name of the command.
     */
    String getName();

    String getSessionId();

    /**
     * Returns the list of notes that the command has.
     *
     * @return the notes.
     */
    List<AdHocCommandNote> getNotes();

    /**
     * Returns the form of the command.
     *
     * @return the data form associated with the command.
     */
    DataForm getForm();

    /**
     * Returns the action to execute. The action is set only on a request.
     *
     * @return the action to execute.
     */
    Action getAction();

    /**
     * Returns the status of the execution.
     *
     * @return the status.
     */
    Status getStatus();

    Set<AllowedAction> getActions();

    AllowedAction getExecuteAction();

    default boolean isCompleted() {
        return getStatus() == Status.completed;
    }

    default boolean isExecuting() {
        return getStatus() == Status.executing;
    }
}
