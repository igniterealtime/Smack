/**
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
package org.jivesoftware.smackx.xdata.form;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.packet.DataForm.Type;

public abstract class FilledForm implements FormReader {

    private final DataForm dataForm;

    protected final TextSingleFormField formTypeFormField;

    public FilledForm(DataForm dataForm) {
        this.dataForm = Objects.requireNonNull(dataForm);
        String formType = dataForm.getFormType();
        if (StringUtils.isNullOrEmpty(formType)) {
            throw new IllegalArgumentException("The provided data form has no hidden FROM_TYPE field.");
        }
        if (dataForm.getType() == Type.cancel) {
            throw new IllegalArgumentException("Forms of type 'cancel' are not filled nor fillable");
        }
        formTypeFormField = dataForm.getHiddenFormTypeField();
    }

    @Override
    public FormField read(String fieldName) {
        return dataForm.getField(fieldName);
    }

    public String getTitle() {
        return dataForm.getTitle();
    }

    public StringBuilder getInstructions() {
        StringBuilder sb = new StringBuilder();
        for (String instruction : dataForm.getInstructions()) {
            sb.append(instruction).append('\n');
        }
        return sb;
    }

    public DataForm getDataForm() {
        return dataForm;
    }

    public String getFormType() {
        if (formTypeFormField == null) {
            return null;
        }
        return formTypeFormField.getValue();
    }

    public boolean hasField(String fieldName) {
        return dataForm.hasField(fieldName);
    }

    public FormField getField(String fieldName) {
        return dataForm.getField(fieldName);
    }

    protected FormField getFieldOrThrow(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            throw new IllegalArgumentException("No field named " + fieldName);
        }
        return formField;
    }

    protected static void ensureFormType(DataForm dataForm, String formType) {
        String dataFormType = dataForm.getFormType();
        if (!formType.equals(dataFormType)) {
            throw new IllegalArgumentException("The provided data form must be of type '" + formType
                            + "', this one was of type '" + dataFormType + '\'');
        }
    }

}
