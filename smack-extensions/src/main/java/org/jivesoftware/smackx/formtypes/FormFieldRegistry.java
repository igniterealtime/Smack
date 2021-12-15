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
package org.jivesoftware.smackx.formtypes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlUtil;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class FormFieldRegistry {

    private static final Logger LOGGER = Logger.getLogger(FormFieldRegistry.class.getName());

    private static final Map<String, Map<String, FormField.Type>> REGISTRY = new HashMap<>();

    private static final Map<String, FormField.Type> CLARK_NOTATION_FIELD_REGISTRY = new ConcurrentHashMap<>();

    private static final Map<String, FormField.Type> LOOKASIDE_FIELD_REGISTRY = new ConcurrentHashMap<>();

    @SuppressWarnings("ReferenceEquality")
    public static void register(DataForm dataForm) {
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

            FormField.Type type = formField.getType();
            if (type == FormField.Type.fixed) {
                continue;
            }

            String fieldName = formField.getFieldName();
            register(formType, fieldName, type);
        }
    }

    public static void register(String formType, FormField.Type fieldType, String... fieldNames) {
        for (String fieldName : fieldNames) {
            register(formType, fieldName, fieldType);
        }
    }

    public static void register(String formType, String fieldName, FormField.Type fieldType) {
        StringUtils.requireNotNullNorEmpty(fieldName, "fieldName must be provided");
        Objects.requireNonNull(fieldType);

        if (formType == null) {
            if (XmlUtil.isClarkNotation(fieldName)) {
                CLARK_NOTATION_FIELD_REGISTRY.put(fieldName, fieldType);
            }
            return;
        }

        FormField.Type previousType;
        synchronized (REGISTRY) {
            Map<String, FormField.Type> fieldNameToType = REGISTRY.get(formType);
            if (fieldNameToType == null) {
                fieldNameToType = new HashMap<>();
                REGISTRY.put(formType, fieldNameToType);
            } else {
                previousType = fieldNameToType.get(fieldName);
                if (previousType != null && previousType != fieldType) {
                    throw new IllegalArgumentException();
                }
            }
            previousType = fieldNameToType.put(fieldName, fieldType);
        }
        if (previousType != null && fieldType != previousType) {
            LOGGER.warning("Form field registry inconsitency detected: Registered field '" + fieldName + "' of type " + fieldType + " but previous type was " + previousType);
        }

    }

    public static FormField.Type lookup(String formType, String fieldName) {
        if (formType == null) {
            if (XmlUtil.isClarkNotation(fieldName)) {
                return CLARK_NOTATION_FIELD_REGISTRY.get(fieldName);
            }

            return LOOKASIDE_FIELD_REGISTRY.get(fieldName);
        }

        synchronized (REGISTRY) {
            Map<String, FormField.Type> fieldNameToTypeMap = REGISTRY.get(formType);
            if (fieldNameToTypeMap != null) {
                FormField.Type type = fieldNameToTypeMap.get(fieldName);
                if (type != null) {
                    return type;
                }
            }
        }

        return null;
    }

    public static synchronized FormField.Type lookup(String fieldName) {
        return lookup(null, fieldName);
    }

    public static void addLookasideFieldRegistryEntry(String fieldName, FormField.Type formFieldType) {
        LOOKASIDE_FIELD_REGISTRY.put(fieldName, formFieldType);
    }
}
