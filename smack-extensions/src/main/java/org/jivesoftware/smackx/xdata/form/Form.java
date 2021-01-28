/**
 *
 * Copyright 2020-2021 Florian Schmaus
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

import org.jivesoftware.smack.packet.StanzaView;

import org.jivesoftware.smackx.xdata.packet.DataForm;

public class Form extends FilledForm {

    public Form(DataForm dataForm) {
        super(dataForm);
        if (dataForm.getType() != DataForm.Type.form) {
            throw new IllegalArgumentException();
        }
    }

    public FillableForm getFillableForm() {
        return new FillableForm(getDataForm());
    }

    public static Form from(StanzaView stanzaView) {
        DataForm dataForm = DataForm.from(stanzaView);
        if (dataForm == null || dataForm.getType() != DataForm.Type.form) {
            return null;
        }
        return new Form(dataForm);
    }
}
