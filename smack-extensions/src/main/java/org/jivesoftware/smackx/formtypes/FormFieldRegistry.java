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
package org.jivesoftware.smackx.formtypes;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.util.Objects;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class FormFieldRegistry {

    private static final Map<String, Map<String, FormField.Type>> REGISTRY = new HashMap<>();

    private static final Map<String, FormField.Type> LOOKASIDE_REGISTRY = new HashMap<>();

    private static final Map<String, String> FIELD_NAME_TO_FORM_TYPE = new HashMap<>();

    static {
        register(FormField.FORM_TYPE, FormField.Type.hidden);
    }

    @SuppressWarnings("ReferenceEquality")
    public static synchronized void register(DataForm dataForm) {
        // TODO: Also allow forms of type 'result'?
        if (dataForm.getType() != DataForm.Type.form) {
            throw new IllegalArgumentException();
        }

        String formType = null;
        TextSingleFormField hiddenFormTypeField = dataForm.getHiddenFormTypeField();
        if (hiddenFormTypeField != null) {
            formType = hiddenFormTypeField.getValue();
        }

        for (FormField formField : dataForm.getFields()) {
            // Note that we can compare here by reference equality to skip the hidden form type field.
            if (formField == hiddenFormTypeField) {
                continue;
            }

            String fieldName = formField.getFieldName();
            FormField.Type type = formField.getType();
            register(formType, fieldName, type);
        }
    }

    public static synchronized void register(String formType, String fieldName, FormField.Type type) {
        if (formType == null) {
            FormFieldInformation formFieldInformation = lookup(fieldName);
            if (formFieldInformation != null) {
                if (Objects.equals(formType, formFieldInformation.formType)
                                && type.equals(formFieldInformation.formFieldType)) {
                    // The field is already registered, nothing to do here.
                    return;
                }

                String message = "There is already a field with the name'" + fieldName
                                + "' registered with the field type '" + formFieldInformation.formFieldType
                                + "', while this tries to register the field with the type '" + type + '\'';
                throw new IllegalArgumentException(message);
            }

            LOOKASIDE_REGISTRY.put(fieldName, type);
            return;
        }

        Map<String, FormField.Type> fieldNameToType = REGISTRY.get(formType);
        if (fieldNameToType == null) {
            fieldNameToType = new HashMap<>();
            REGISTRY.put(formType, fieldNameToType);
        } else {
            FormField.Type previousType = fieldNameToType.get(fieldName);
            if (previousType != null && previousType != type) {
                throw new IllegalArgumentException();
            }
        }
        fieldNameToType.put(fieldName, type);

        FIELD_NAME_TO_FORM_TYPE.put(fieldName, formType);
    }

    public static synchronized void register(String fieldName, FormField.Type type) {
        FormField.Type previousType = LOOKASIDE_REGISTRY.get(fieldName);
        if (previousType != null) {
            if (previousType == type) {
                // Nothing to do here.
                return;
            }
            throw new IllegalArgumentException("There is already a field with the name '" + fieldName
                            + "' registered with type " + previousType
                            + ", while trying to register this field with type '" + type + "'");
        }

        LOOKASIDE_REGISTRY.put(fieldName, type);
    }

    public static synchronized FormField.Type lookup(String formType, String fieldName) {
        if (formType != null) {
            Map<String, FormField.Type> fieldNameToTypeMap = REGISTRY.get(formType);
            if (fieldNameToTypeMap != null) {
                FormField.Type type = fieldNameToTypeMap.get(fieldName);
                if (type != null) {
                    return type;
                }
            }
        } else {
            formType = FIELD_NAME_TO_FORM_TYPE.get(fieldName);
            if (formType != null) {
                FormField.Type type = lookup(formType, fieldName);
                if (type != null) {
                    return type;
                }
            }
        }

        // Fallback to lookaside registry.
        return LOOKASIDE_REGISTRY.get(fieldName);
    }

    public static synchronized FormFieldInformation lookup(String fieldName) {
        String formType = FIELD_NAME_TO_FORM_TYPE.get(fieldName);
        FormField.Type type = lookup(formType, fieldName);
        if (type == null) {
            return null;
        }

        return new FormFieldInformation(type, formType);
    }

    public static final class FormFieldInformation {
        public final FormField.Type formFieldType;
        public final String formType;


        private FormFieldInformation(FormField.Type formFieldType, String formType) {
            this.formFieldType = formFieldType;
            this.formType = formType;
        }
    }
}
