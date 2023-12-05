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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder.NextStage;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder.PreviousStage;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class AdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    public AdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void singleStageAdHocCommandTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AdHocCommandManager manOne = AdHocCommandManager.getInstance(conOne);
        AdHocCommandManager manTwo = AdHocCommandManager.getInstance(conTwo);

        String commandNode = "test-list";
        String commandName = "Return a list for testing purposes";
        AdHocCommandHandlerFactory factory = (String node, String name, String sessionId) -> {
            return new AdHocCommandHandler.SingleStage(node, name, sessionId) {
                @Override
                public AdHocCommandData executeSingleStage(AdHocCommandDataBuilder response) {
                    FormField field = FormField.textPrivateBuilder("my-field").build();
                    DataForm form = DataForm.builder(DataForm.Type.result).addField(field).build();

                    response.setForm(form);

                    return response.build();
                }
            };
        };
        manOne.registerCommand(commandNode, commandName, factory);
        try {
            AdHocCommand command = manTwo.getRemoteCommand(conOne.getUser(), commandNode);

            AdHocCommandResult result = command.execute();
            AdHocCommandData response = result.getResponse();
            DataForm form = response.getForm();
            FormField field = form.getField("my-field");
            assertNotNull(field);
        } finally {
            manOne.unregisterCommand(commandNode);
        }
    }

    private static class MyMultiStageAdHocCommandServer extends AdHocCommandHandler {

        private Integer a;
        private Integer b;

        private static DataForm createDataForm(String variableName) {
            FormField field = FormField.textSingleBuilder(variableName).setRequired().build();
            return DataForm.builder(DataForm.Type.form)
               .setTitle("Variable " + variableName)
               .setInstructions("Please provide an integer variable " + variableName)
               .addField(field)
               .build();
        }

        private static DataForm createDataFormOp() {
            FormField field = FormField.listSingleBuilder("op")
                            .setLabel("Arthimetic Operation")
                            .setRequired()
                            .addOption("+")
                            .addOption("-")
                            .build();
            return DataForm.builder(DataForm.Type.form)
               .setTitle("Operation")
               .setInstructions("Please select the arithmetic operation to be performed with a and b")
               .addField(field)
               .build();
        }
        private static final DataForm dataFormAskingForA = createDataForm("a");
        private static final DataForm dataFormAskingForB = createDataForm("b");
        private static final DataForm dataFormAskingForOp = createDataFormOp();

        MyMultiStageAdHocCommandServer(String node, String name, String sessionId) {
            super(node, name, sessionId);
        }

        @Override
        protected AdHocCommandData execute(AdHocCommandDataBuilder response) throws XMPPErrorException {
            return response.setForm(dataFormAskingForA).setStatusExecuting(PreviousStage.none,
                            NextStage.nonFinal).build();
        }

        // TODO: Add API for every case where we return null or throw below.
        private static Integer extractIntegerField(SubmitForm form, String fieldName) throws XMPPErrorException {
            FormField field = form.getField(fieldName);
            if (field == null)
                throw newBadRequestException("Submitted form does not contain a field of name " + fieldName);

            String fieldValue = field.getFirstValue();
            if (fieldValue == null)
                throw newBadRequestException("Submitted form contains field of name " + fieldName + " without value");

            try {
                return Integer.parseInt(fieldValue);
            } catch (NumberFormatException e) {
                throw newBadRequestException("Submitted form contains field of name " + fieldName + " with value " + fieldValue + " that is not an integer");
            }
        }

        @Override
        protected AdHocCommandData next(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                        throws XMPPErrorException {
            DataForm form;
            switch (getCurrentStage()) {
            case 2:
                a = extractIntegerField(submittedForm, "a");
                form = dataFormAskingForB;
                response.setStatusExecuting(PreviousStage.exists, NextStage.nonFinal);
                break;
            case 3:
                b = extractIntegerField(submittedForm, "b");
                form = dataFormAskingForOp;
                response.setStatusExecuting(PreviousStage.exists, NextStage.isFinal);
                break;
            case 4:
                // Ad-Hoc Commands particularity: Can get to 'complete' via 'next'.
                return complete(response, submittedForm);
            default:
                throw new IllegalStateException();
            }

            return response.setForm(form).build();
        }

        @Override
        protected AdHocCommandData complete(AdHocCommandDataBuilder response, SubmitForm submittedForm)
                        throws XMPPErrorException {
            if (getCurrentStage() != 4) {
                throw new IllegalStateException();
            }

            if (a == null || b == null) {
                throw new IllegalStateException();
            }

            String op = submittedForm.getField("op").getFirstValue();

            int result;
            switch (op) {
            case "+":
                result = a + b;
                break;
            case "-":
                result = a - b;
                break;
            default:
                throw newBadRequestException("Submitted operation " + op + " is neither + nor -");
            }

            response.setStatusCompleted();

            FormField field = FormField.textSingleBuilder("result").setValue(result).build();
            DataForm form = DataForm.builder(DataForm.Type.result).setTitle("Result").addField(field).build();

            return response.setForm(form).build();
        }

        @Override
        protected AdHocCommandData prev(AdHocCommandDataBuilder response) throws XMPPErrorException {
            switch (getCurrentStage()) {
            case 1:
                return execute(response);
            case 2:
                return response.setForm(dataFormAskingForA)
                                .setStatusExecuting(PreviousStage.exists, NextStage.nonFinal)
                                .build();
            case 3:
                return response.setForm(dataFormAskingForB)
                                .setStatusExecuting(PreviousStage.exists, NextStage.isFinal)
                                .build();
            default:
                throw new IllegalStateException();
            }
        }

        @Override
        public void cancel() {
        }

    }

    @SmackIntegrationTest
    public void multiStageAdHocCommandTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AdHocCommandManager manOne = AdHocCommandManager.getInstance(conOne);
        AdHocCommandManager manTwo = AdHocCommandManager.getInstance(conTwo);

        String commandNode = "my-multi-stage-command";
        String commandName = "An example multi-sage ad-hoc command";
        AdHocCommandHandlerFactory factory = (String node, String name, String sessionId) -> {
            return new MyMultiStageAdHocCommandServer(node, name, sessionId);
        };
        manOne.registerCommand(commandNode, commandName, factory);

        try {
            AdHocCommand command = manTwo.getRemoteCommand(conOne.getUser(), commandNode);

            AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();

            FillableForm form = result.getFillableForm();
            form.setAnswer("a", 42);

            SubmitForm submitForm = form.getSubmitForm();


            result = command.next(submitForm).asExecutingOrThrow();

            form = result.getFillableForm();
            form.setAnswer("b", 23);

            submitForm = form.getSubmitForm();


            result = command.next(submitForm).asExecutingOrThrow();

            form = result.getFillableForm();
            form.setAnswer("op", "+");

            submitForm = form.getSubmitForm();

            AdHocCommandResult.StatusCompleted completed = command.complete(submitForm).asCompletedOrThrow();

            String operationResult = completed.getResponse().getForm().getField("result").getFirstValue();
            assertEquals("65", operationResult);
        } finally {
            manTwo.unregisterCommand(commandNode);
        }
    }

    @SmackIntegrationTest
    public void multiStageWithPrevAdHocCommandTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AdHocCommandManager manOne = AdHocCommandManager.getInstance(conOne);
        AdHocCommandManager manTwo = AdHocCommandManager.getInstance(conTwo);

        String commandNode = "my-multi-stage-with-prev-command";
        String commandName = "An example multi-sage ad-hoc command";
        AdHocCommandHandlerFactory factory = (String node, String name, String sessionId) -> {
            return new MyMultiStageAdHocCommandServer(node, name, sessionId);
        };
        manOne.registerCommand(commandNode, commandName, factory);

        try {
            AdHocCommand command = manTwo.getRemoteCommand(conOne.getUser(), commandNode);

            AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();

            FillableForm form = result.getFillableForm();
            form.setAnswer("a", 42);

            SubmitForm submitForm = form.getSubmitForm();

            command.next(submitForm).asExecutingOrThrow();


            // Ups, I wanted a different value for 'a', lets execute 'prev' to get back to the previous stage.
            result = command.prev().asExecutingOrThrow();

            form = result.getFillableForm();
            form.setAnswer("a", 77);

            submitForm = form.getSubmitForm();


            result = command.next(submitForm).asExecutingOrThrow();

            form = result.getFillableForm();
            form.setAnswer("b", 23);

            submitForm = form.getSubmitForm();


            result = command.next(submitForm).asExecutingOrThrow();

            form = result.getFillableForm();
            form.setAnswer("op", "+");

            submitForm = form.getSubmitForm();

            AdHocCommandResult.StatusCompleted completed = command.complete(submitForm).asCompletedOrThrow();

            String operationResult = completed.getResponse().getForm().getField("result").getFirstValue();
            assertEquals("100", operationResult);
        } finally {
            manTwo.unregisterCommand(commandNode);
        }
    }

    @SmackIntegrationTest
    public void multiStageInvalidArgAdHocCommandTest() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        AdHocCommandManager manOne = AdHocCommandManager.getInstance(conOne);
        AdHocCommandManager manTwo = AdHocCommandManager.getInstance(conTwo);

        String commandNode = "my-multi-stage-invalid-arg-command";
        String commandName = "An example multi-sage ad-hoc command";
        AdHocCommandHandlerFactory factory = (String node, String name, String sessionId) -> {
            return new MyMultiStageAdHocCommandServer(node, name, sessionId);
        };
        manOne.registerCommand(commandNode, commandName, factory);

        try {
            AdHocCommand command = manTwo.getRemoteCommand(conOne.getUser(), commandNode);

            AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();

            FillableForm form = result.getFillableForm();
            form.setAnswer("a", "forty-two");

            SubmitForm submitForm = form.getSubmitForm();

            XMPPErrorException exception = assertThrows(XMPPErrorException.class, () -> command.next(submitForm));
            assertEquals(exception.getStanzaError().getCondition(), StanzaError.Condition.bad_request);
        } finally {
            manTwo.unregisterCommand(commandNode);
        }
    }
}
