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
package org.jivesoftware.smackx.commands;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

// TODO: Make this a sealed class once Smack is Java 17 or higher.
public abstract class AdHocCommandResult {

    private final AdHocCommandData response;
    private final boolean completed;

    private AdHocCommandResult(AdHocCommandData response, boolean completed) {
        this.response = response;
        this.completed = completed;
    }

    public final AdHocCommandData getResponse() {
        return response;
    }

    public final boolean isCompleted() {
        return completed;
    }

    public StatusExecuting asExecutingOrThrow() {
        if (this instanceof StatusExecuting)
            return (StatusExecuting) this;

        throw new IllegalStateException();
    }

    public StatusCompleted asCompletedOrThrow() {
        if (this instanceof StatusCompleted)
            return (StatusCompleted) this;

        throw new IllegalStateException();
    }

    public static final class StatusExecuting extends AdHocCommandResult {
        private StatusExecuting(AdHocCommandData response) {
            super(response, false);
            assert response.getStatus() == AdHocCommandData.Status.executing;
        }

        public FillableForm getFillableForm() {
            DataForm form = getResponse().getForm();
            return new FillableForm(form);
        }
    }

    public static final class StatusCompleted extends AdHocCommandResult {
        private StatusCompleted(AdHocCommandData response) {
            super(response, true);
            assert response.getStatus() == AdHocCommandData.Status.completed;
        }
    }

    /**
     * This subclass is only used internally by Smack.
     */
    @SuppressWarnings("JavaLangClash")
    static final class Error extends AdHocCommandResult {
        private Error(AdHocCommandData response) {
            super(response, false);
        }
    }

    public static AdHocCommandResult from(AdHocCommandData response) {
        IQ.Type iqType = response.getType();
        if (iqType == IQ.Type.error)
            return new Error(response);

        assert iqType == IQ.Type.result;

        switch (response.getStatus()) {
        case executing:
            return new StatusExecuting(response);
        case completed:
            return new StatusCompleted(response);
        default:
            throw new IllegalArgumentException();
        }
    }
}
