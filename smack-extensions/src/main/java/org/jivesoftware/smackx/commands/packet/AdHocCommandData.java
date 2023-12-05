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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlElement;

import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.SpecificErrorCondition;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * Represents the state and the request of the execution of an adhoc command.
 *
 * @author Gabriel Guardincerri
 * @author Florian Schmaus
 */
public class AdHocCommandData extends IQ implements AdHocCommandDataView {

    public static final String ELEMENT = "command";
    public static final String NAMESPACE = "http://jabber.org/protocol/commands";

    private final String node;

    private final String name;

    private final String sessionId;

    private final List<AdHocCommandNote> notes = new ArrayList<>();

    private final DataForm form;

    private final Action action;

    private final Status status;

    private final Set<AllowedAction> actions = EnumSet.noneOf(AllowedAction.class);

    private final AllowedAction executeAction;

    public AdHocCommandData(AdHocCommandDataBuilder builder) {
        super(builder, ELEMENT, NAMESPACE);
        node = builder.getNode();
        name = builder.getName();
        sessionId = builder.getSessionId();
        notes.addAll(builder.getNotes());
        form = builder.getForm();
        action = builder.getAction();
        status = builder.getStatus();
        actions.addAll(builder.getActions());
        executeAction = builder.getExecuteAction();

        if (executeAction != null && !actions.contains(executeAction)) {
            throw new IllegalArgumentException("Execute action " + executeAction + " is not part of allowed actions: " + actions);
        }
    }

    @Override
    public String getNode() {
        return node;
    }

   @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public List<AdHocCommandNote> getNotes() {
        return notes;
    }

    @Override
    public DataForm getForm() {
        return form;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Set<AllowedAction> getActions() {
        return actions;
    }

    @Override
    public AllowedAction getExecuteAction() {
        return executeAction;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("node", node);
        xml.optAttribute("sessionid", sessionId);
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

                for (AdHocCommandData.AllowedAction action : actions) {
                    xml.emptyElement(action);
                }
                xml.closeElement("actions");
            }
        }

        xml.optAppend(form);

        for (AdHocCommandNote note : notes) {
            xml.halfOpenElement("note")
              .attribute("type", note.getType().toString())
              .rightAngleBracket();
            xml.append(note.getValue());
            xml.closeElement("note");
        }

        // TODO ERRORS
//        if (getError() != null) {
//            buf.append(getError().toXML());
//        }

        return xml;
    }

    public static AdHocCommandDataBuilder builder(String node, IqData iqCommon) {
        return new AdHocCommandDataBuilder(node, iqCommon);
    }

    public static AdHocCommandDataBuilder builder(String node, String stanzaId) {
        return new AdHocCommandDataBuilder(node, stanzaId);
    }

    public static AdHocCommandDataBuilder builder(String node, XMPPConnection connection) {
        return new AdHocCommandDataBuilder(node, connection);
    }

    public static class SpecificError implements XmlElement {

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
        public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            StringBuilder buf = new StringBuilder();
            buf.append('<').append(getElementName());
            buf.append(" xmlns=\"").append(getNamespace()).append("\"/>");
            return buf.toString();
        }
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

    public enum AllowedAction {

        /**
         * The command should be digress to the previous stage of execution.
         */
        prev(Action.prev),

        /**
         * The command should progress to the next stage of execution.
         */
        next(Action.next),

        /**
         * The command should be completed (if possible).
         */
        complete(Action.complete),
        ;

        public final Action action;

        AllowedAction(Action action) {
            this.action = action;
        }
    }

    public enum Action {
        /**
         * The command should be executed or continue to be executed. This is
         * the default value.
         */
        execute(null),

        /**
         * The command should be canceled.
         */
        cancel(null),

        /**
         * The command should be digress to the previous stage of execution.
         */
        prev(AllowedAction.prev),

        /**
         * The command should progress to the next stage of execution.
         */
        next(AllowedAction.next),

        /**
         * The command should be completed (if possible).
         */
        complete(AllowedAction.complete),
        ;

        public final AllowedAction allowedAction;

        Action(AllowedAction allowedAction) {
            this.allowedAction = allowedAction;
        }

    }
}
