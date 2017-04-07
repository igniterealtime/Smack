/**
 *
 * Copyright 2005-2008 Jive Software.
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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommand.SpecificErrorCondition;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state and the request of the execution of an adhoc command.
 * 
 * @author Gabriel Guardincerri
 */
public class AdHocCommandData extends IQ {

    public static final String ELEMENT = "command";
    public static final String NAMESPACE = "http://jabber.org/protocol/commands";

    /* JID of the command host */
    private Jid id;

    /* Command name */
    private String name;

    /* Command identifier */
    private String node;

    /* Unique ID of the execution */
    private String sessionID;

    private List<AdHocCommandNote> notes = new ArrayList<AdHocCommandNote>();

    private DataForm form;

    /* Action request to be executed */
    private AdHocCommand.Action action;

    /* Current execution status */
    private AdHocCommand.Status status;

    private ArrayList<AdHocCommand.Action> actions = new ArrayList<AdHocCommand.Action>();

    private AdHocCommand.Action executeAction;

    public AdHocCommandData() {
        super(ELEMENT, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("node", node);
        xml.optAttribute("sessionid", sessionID);
        xml.optAttribute("status", status);
        xml.optAttribute("action", action);
        xml.rightAngleBracket();

        if (getType() == Type.result) {
            xml.halfOpenElement("actions");
            xml.optAttribute("execute", executeAction);
            if (actions.size() == 0) {
                xml.closeEmptyElement();
            } else {
                xml.rightAngleBracket();

                for (AdHocCommand.Action action : actions) {
                    xml.emptyElement(action);
                }
                xml.closeElement("actions");
            }
        }

        if (form != null) {
            xml.append(form.toXML());
        }

        for (AdHocCommandNote note : notes) {
            xml.halfOpenElement("note").attribute("type", note.getType().toString()).rightAngleBracket();
            xml.append(note.getValue());
            xml.closeElement("note");
        }

        // TODO ERRORS
//        if (getError() != null) {
//            buf.append(getError().toXML());
//        }

        return xml;
    }

    /**
     * Returns the JID of the command host.
     *
     * @return the JID of the command host.
     */
    public Jid getId() {
        return id;
    }

    public void setId(Jid id) {
        this.id = id;
    }

    /**
     * Returns the human name of the command.
     *
     * @return the name of the command.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the identifier of the command.
     *
     * @return the node.
     */
    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    /**
     * Returns the list of notes that the command has.
     *
     * @return the notes.
     */
    public List<AdHocCommandNote> getNotes() {
        return notes;
    }

    public void addNote(AdHocCommandNote note) {
        this.notes.add(note);
    }

    public void remveNote(AdHocCommandNote note) {
        this.notes.remove(note);
    }

    /**
     * Returns the form of the command.
     *
     * @return the data form associated with the command.
     */
    public DataForm getForm() {
        return form;
    }

    public void setForm(DataForm form) {
        this.form = form;
    }

    /**
     * Returns the action to execute. The action is set only on a request.
     *
     * @return the action to execute.
     */
    public AdHocCommand.Action getAction() {
        return action;
    }

    public void setAction(AdHocCommand.Action action) {
        this.action = action;
    }

    /**
     * Returns the status of the execution.
     *
     * @return the status.
     */
    public AdHocCommand.Status getStatus() {
        return status;
    }

    public void setStatus(AdHocCommand.Status status) {
        this.status = status;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void setExecuteAction(Action executeAction) {
        this.executeAction = executeAction;
    }

    public Action getExecuteAction() {
        return executeAction;
    }

    /**
     * Set the 'sessionid' attribute of the command.
     * <p>
     * This value can be null or empty for the first command, but MUST be set for subsequent commands. See also <a
     * href="http://xmpp.org/extensions/xep-0050.html#impl-session">XEP-0050 § 3.3 Session Lifetime</a>.
     * </p>
     *
     * @param sessionID
     */
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getSessionID() {
        return sessionID;
    }

    public static class SpecificError implements ExtensionElement {

        public static final String namespace = "http://jabber.org/protocol/commands";

        public SpecificErrorCondition condition;

        public SpecificError(SpecificErrorCondition condition) {
            this.condition = condition;
        }

        @Override
        public String getElementName() {
            return condition.toString();
        }
        @Override
        public String getNamespace() {
            return namespace;
        }

        public SpecificErrorCondition getCondition() {
            return condition;
        }

        @Override
        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append('<').append(getElementName());
            buf.append(" xmlns=\"").append(getNamespace()).append("\"/>");
            return buf.toString();
        }
    }
}
