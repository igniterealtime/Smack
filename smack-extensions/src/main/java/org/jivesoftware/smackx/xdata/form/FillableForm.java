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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.xdata.AbstractMultiFormField;
import org.jivesoftware.smackx.xdata.AbstractSingleStringValueFormField;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormField.Type;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppDateTime;

public class FillableForm extends FilledForm {

    private final Set<String> requiredFields;

    private final Set<String> filledRequiredFields = new HashSet<>();
    private final Set<String> missingRequiredFields = new HashSet<>();

    private final Map<String, FormField> filledFields = new HashMap<>();

    public FillableForm(DataForm dataForm) {
        super(dataForm);
        if (dataForm.getType() != DataForm.Type.form) {
            throw new IllegalArgumentException();
        }

        Set<String> requiredFields = new HashSet<>();
        for (FormField formField : dataForm.getFields()) {
            if (formField.isRequired()) {
                String fieldName = formField.getFieldName();
                requiredFields.add(fieldName);

                if (formField.hasValueSet()) {
                    // This is a form field with a default value.
                    write(formField);
                } else {
                    missingRequiredFields.add(fieldName);
                }
            }
        }
        this.requiredFields = Collections.unmodifiableSet(requiredFields);
    }

    protected void writeListMulti(String fieldName, List<? extends CharSequence> values) {
        FormField formField = FormField.listMultiBuilder(fieldName)
                        .addValues(values)
                        .build();
        write(formField);
    }

    protected void writeTextSingle(String fieldName, CharSequence value) {
        FormField formField = FormField.textSingleBuilder(fieldName)
                        .setValue(value)
                        .build();
        write(formField);
    }

    protected void writeBoolean(String fieldName, boolean value) {
        FormField formField = FormField.booleanBuilder(fieldName)
                        .setValue(value)
                        .build();
        write(formField);
    }

    protected void write(String fieldName, int value) {
        writeTextSingle(fieldName, Integer.toString(value));
    }

    protected void write(String fieldName, Date date) {
        writeTextSingle(fieldName, XmppDateTime.formatXEP0082Date(date));
    }

    public void setAnswer(String fieldName, Collection<? extends CharSequence> answers) {
        FormField blankField = getFieldOrThrow(fieldName);
        FormField.Type type = blankField.getType();

        FormField filledFormField;
        switch (type) {
        case list_multi:
        case text_multi:
            filledFormField = createMultiKindFieldbuilder(fieldName, type)
                .addValues(answers)
                .build();
            break;
        case jid_multi:
            List<Jid> jids = new ArrayList<>(answers.size());
            List<XmppStringprepException> exceptions = new ArrayList<>();
            JidUtil.jidsFrom(answers, jids, exceptions);
            if (!exceptions.isEmpty()) {
                // TODO: Report all exceptions here.
                throw new IllegalArgumentException(exceptions.get(0));
            }
            filledFormField = FormField.jidMultiBuilder(fieldName)
                            .addValues(jids)
                            .build();
            break;
        default:
            throw new IllegalArgumentException("");
        }
        write(filledFormField);
    }

    private static AbstractMultiFormField.Builder<?, ?> createMultiKindFieldbuilder(String fieldName, FormField.Type type) {
        switch (type) {
        case list_multi:
            return FormField.listMultiBuilder(fieldName);
        case text_multi:
            return FormField.textMultiBuilder(fieldName);
        default:
            throw new IllegalArgumentException();
        }
    }

    public void setAnswer(String fieldName, int answer) {
        setAnswer(fieldName, Integer.toString(answer));
    }

    public void setAnswer(String fieldName, CharSequence answer) {
        FormField blankField = getFieldOrThrow(fieldName);
        FormField.Type type = blankField.getType();

        FormField filledFormField;
        switch (type) {
        case list_multi:
        case jid_multi:
            throw new IllegalArgumentException("Can not answer fields of type '" + type + "' with a CharSequence");
        case fixed:
            throw new IllegalArgumentException("Fields of type 'fixed' are not answerable");
        case list_single:
        case text_private:
        case text_single:
        case hidden:
            filledFormField = createSingleKindFieldBuilder(fieldName, type)
                .setValue(answer)
                .build();
            break;
        case bool:
            filledFormField = FormField.booleanBuilder(fieldName)
                .setValue(answer)
                .build();
            break;
        case jid_single:
            Jid jid;
            try {
                jid = JidCreate.from(answer);
            } catch (XmppStringprepException e) {
                throw new IllegalArgumentException(e);
            }
            filledFormField = FormField.jidSingleBuilder(fieldName)
                .setValue(jid)
                .build();
            break;
        case text_multi:
            filledFormField = createMultiKindFieldbuilder(fieldName, type)
                .addValue(answer)
                .build();
            break;
        default:
            throw new AssertionError();
        }
        write(filledFormField);
    }

    private static AbstractSingleStringValueFormField.Builder<?, ?> createSingleKindFieldBuilder(String fieldName, FormField.Type type) {
        switch (type) {
        case text_private:
            return FormField.textPrivateBuilder(fieldName);
        case text_single:
            return FormField.textSingleBuilder(fieldName);
        case hidden:
            return FormField.hiddenBuilder(fieldName);
        case list_single:
            return FormField.listSingleBuilder(fieldName);
        default:
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public void setAnswer(String fieldName, boolean answer) {
        FormField blankField = getFieldOrThrow(fieldName);
        if (blankField.getType() != Type.bool) {
            throw new IllegalArgumentException();
        }

        FormField filledFormField = FormField.booleanBuilder(fieldName)
                        .setValue(answer)
                        .build();
        write(filledFormField);
    }

    public final void write(FormField filledFormField) {
        if (filledFormField.getType() == FormField.Type.fixed) {
            throw new IllegalArgumentException();
        }
        if (!filledFormField.hasValueSet()) {
            throw new IllegalArgumentException();
        }

        String fieldName = filledFormField.getFieldName();
        if (!getDataForm().hasField(fieldName)) {
            throw new IllegalArgumentException();
        }

        // Perform validation, e.g. using XEP-0122.
        // TODO: We could also perform list-* option validation, but this has to take xep122's <open/> into account.
        FormField formFieldPrototype = getDataForm().getField(fieldName);
        for (FormFieldChildElement formFieldChildelement : formFieldPrototype.getFormFieldChildElements()) {
            formFieldChildelement.validate(filledFormField);
        }

        filledFields.put(fieldName, filledFormField);
        if (requiredFields.contains(fieldName)) {
            filledRequiredFields.add(fieldName);
            missingRequiredFields.remove(fieldName);
        }
    }

    @Override
    public FormField getField(String fieldName) {
        FormField filledField = filledFields.get(fieldName);
        if (filledField != null) {
            return filledField;
        }

        return super.getField(fieldName);
    }

    public DataForm getDataFormToSubmit() {
        if (!missingRequiredFields.isEmpty()) {
            throw new IllegalStateException("Not all required fields filled. Missing: " + missingRequiredFields);
        }
        DataForm.Builder builder = DataForm.builder();

        // the submit form has the same FORM_TYPE as the form.
        if (formTypeFormField != null) {
            builder.addField(formTypeFormField);
        }

        builder.addFields(filledFields.values());

        return builder.build();
    }

}
