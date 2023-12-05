/**
 *
 * Copyright 2024 Florian Schmaus.
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
package org.jivesoftware.smackx.xdata.form;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class FillableFormTest {

    @Test
    public void testThrowOnIncompleteyFilled() {
        FormField fieldA = FormField.textSingleBuilder("a").setRequired().build();
        FormField fieldB = FormField.textSingleBuilder("b").setRequired().build();
        DataForm form = DataForm.builder(DataForm.Type.form)
                        .addField(fieldA)
                        .addField(fieldB)
                        .build();

        FillableForm fillableForm = new FillableForm(form);
        fillableForm.setAnswer("a", 42);

        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> fillableForm.getSubmitForm());
        assertTrue(ise.getMessage().startsWith("Not all required fields filled. "));
    }
}
