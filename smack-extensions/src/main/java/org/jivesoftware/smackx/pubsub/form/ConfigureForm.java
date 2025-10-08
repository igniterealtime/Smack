/*
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub.form;

import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class ConfigureForm extends Form implements ConfigureFormReader {

    public ConfigureForm(DataForm dataForm) {
        super(dataForm);
        ensureFormType(dataForm, FORM_TYPE);
    }

    @Override
    public FillableConfigureForm getFillableForm() {
        return new FillableConfigureForm(getDataForm());
    }
}
