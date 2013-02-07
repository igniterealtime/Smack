/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import org.jivesoftware.smackx.packet.AdHocCommandData;

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
 * </ul><p/>
 * To implement a new command extend this class and implement all the abstract
 * methods. When implementing the actions remember that they could be invoked
 * several times, and that you must use the current stage number to know what to
 * do.
 * 
 * @author Gabriel Guardincerri
 */
public abstract class LocalCommand extends AdHocCommand {

    /**
     * The time stamp of first invokation of the command. Used to implement the session timeout.
     */
    private long creationDate;

    /**
     * The unique ID of the execution of the command.
     */
    private String sessionID;

    /**
     * The full JID of the host of the command.
     */
    private String ownerJID;

    /**
     * The number of the current stage.
     */
    private int currenStage;

    public LocalCommand() {
        super();
        this.creationDate = System.currentTimeMillis();
        currenStage = -1;
    }

    /**
     * The sessionID is an unique identifier of an execution request. This is
     * automatically handled and should not be called.
     * 
     * @param sessionID the unique session id of this execution
     */
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
        getData().setSessionID(sessionID);
    }

    /**
     * Returns the session ID of this execution.
     * 
     * @return the unique session id of this execution
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Sets the JID of the command host. This is automatically handled and should
     * not be called.
     * 
     * @param ownerJID the JID of the owner.
     */
    public void setOwnerJID(String ownerJID) {
        this.ownerJID = ownerJID;
    }

    @Override
    public String getOwnerJID() {
        return ownerJID;
    }

    /**
     * Returns the date the command was created.
     * 
     * @return the date the command was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Returns true if the current stage is the last one. If it is then the
     * execution of some action will complete the execution of the command.
     * Commands that don't have multiple stages can always return <tt>true</tt>.
     * 
     * @return true if the command is in the last stage.
     */
    public abstract boolean isLastStage();

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
    public abstract boolean hasPermission(String jid);

    /**
     * Returns the currently executing stage number. The first stage number is
     * 0. During the execution of the first action this method will answer 0.
     *
     * @return the current stage number.
     */
    public int getCurrentStage() {
        return currenStage;
    }

    @Override
    void setData(AdHocCommandData data) {
        data.setSessionID(sessionID);
        super.setData(data);
    }

    /**
     * Increase the current stage number. This is automatically handled and should
     * not be called.
     * 
     */
    void incrementStage() {
        currenStage++;
    }

    /**
     * Decrease the current stage number. This is automatically handled and should
     * not be called.
     * 
     */
    void decrementStage() {
        currenStage--;
    }
}