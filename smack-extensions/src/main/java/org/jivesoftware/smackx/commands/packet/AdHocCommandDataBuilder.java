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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.AbstractIqBuilder;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Action;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.AllowedAction;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Status;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class AdHocCommandDataBuilder extends IqBuilder<AdHocCommandDataBuilder, AdHocCommandData> implements AdHocCommandDataView {

    private final String node;

    private String name;

    private String sessionId;

    private final List<AdHocCommandNote> notes = new ArrayList<>();

    private DataForm form;

    /* Action request to be executed */
    private Action action;

    /* Current execution status */
    private Status status;

    private final Set<AllowedAction> actions = EnumSet.noneOf(AllowedAction.class);

    private AllowedAction executeAction;

    AdHocCommandDataBuilder(String node, IqData iqCommon) {
        super(iqCommon);
        this.node = StringUtils.requireNotNullNorEmpty(node, "Ad-Hoc Command node must be set");
    }

    AdHocCommandDataBuilder(String node, String stanzaId) {
        super(stanzaId);
        this.node = StringUtils.requireNotNullNorEmpty(node, "Ad-Hoc Command node must be set");
    }

    AdHocCommandDataBuilder(String node, XMPPConnection connection) {
        super(connection);
        this.node = StringUtils.requireNotNullNorEmpty(node, "Ad-Hoc Command node must be set");
    }

    @Override
    public String getNode() {
        return node;
    }

    @Override
    public String getName() {
        return name;
    }

    public AdHocCommandDataBuilder setName(String name) {
        this.name = name;
        return getThis();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    public AdHocCommandDataBuilder setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return getThis();
    }

    @Override
    public List<AdHocCommandNote> getNotes() {
        return notes;
    }

    public AdHocCommandDataBuilder addNote(AdHocCommandNote note) {
        notes.add(note);
        return getThis();
    }

    @Override
    public DataForm getForm() {
        return form;
    }

    public AdHocCommandDataBuilder setForm(DataForm form) {
        this.form = form;
        return getThis();
    }

    @Override
    public Action getAction() {
        return action;
    }

    public AdHocCommandDataBuilder setAction(AdHocCommandData.Action action) {
        this.action = action;
        return getThis();
    }
    @Override

    public AdHocCommandData.Status getStatus() {
        return status;
    }

    public AdHocCommandDataBuilder setStatus(AdHocCommandData.Status status) {
        this.status = status;
        return getThis();
    }

    public AdHocCommandDataBuilder setStatusCompleted() {
        return setStatus(AdHocCommandData.Status.completed);
    }

    public enum PreviousStage {
        exists,
        none,
    }

    public enum NextStage {
        isFinal,
        nonFinal,
    }

    @SuppressWarnings("fallthrough")
    public AdHocCommandDataBuilder setStatusExecuting(PreviousStage previousStage, NextStage nextStage) {
        setStatus(AdHocCommandData.Status.executing);

        switch (previousStage) {
        case exists:
            addAction(AllowedAction.prev);
            break;
        case none:
            break;
        }

        setExecuteAction(AllowedAction.next);

        switch (nextStage) {
        case isFinal:
            addAction(AllowedAction.complete);
            // Override execute action of 'next'.
            setExecuteAction(AllowedAction.complete);
            // Deliberate fallthrough, we want 'next' to be added.
        case nonFinal:
            addAction(AllowedAction.next);
            break;
        }

        return getThis();
    }

    @Override
    public Set<AllowedAction> getActions() {
        return actions;
    }

    public AdHocCommandDataBuilder addAction(AllowedAction action) {
        actions.add(action);
        return getThis();
    }

    @Override
    public AllowedAction getExecuteAction() {
        return executeAction;
    }

    public AdHocCommandDataBuilder setExecuteAction(AllowedAction action) {
        this.executeAction = action;
        return getThis();
    }

    @Override
    public AdHocCommandData build() {
        return new AdHocCommandData(this);
    }

    @Override
    public AdHocCommandDataBuilder getThis() {
        return this;
    }

    public static AdHocCommandDataBuilder buildResponseFor(AdHocCommandData request) {
        return buildResponseFor(request, IQ.ResponseType.result);
    }

    public static AdHocCommandDataBuilder buildResponseFor(AdHocCommandData request, IQ.ResponseType responseType) {
        AdHocCommandDataBuilder builder = new AdHocCommandDataBuilder(request.getNode(), AbstractIqBuilder.createResponse(request, responseType));
        return builder;
    }

}
