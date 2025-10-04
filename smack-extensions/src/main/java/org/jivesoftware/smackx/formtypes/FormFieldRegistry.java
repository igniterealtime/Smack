/**
 *
 * Copyright 2020-2025 Florian Schmaus
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlUtil;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.XDataManager;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class FormFieldRegistry {

    private static final Logger LOGGER = Logger.getLogger(FormFieldRegistry.class.getName());

    private static final Map<String, Map<String, FormField.Type>> REGISTRY = new HashMap<>();

    private static final Map<String, FormField.Type> CLARK_NOTATION_FIELD_REGISTRY = new ConcurrentHashMap<>();

    private static final Map<String, FormField.Type> LOOKASIDE_FIELD_REGISTRY = new ConcurrentHashMap<>();

    static {
        try {
            loadFormFieldRegistryEntries();
        } catch (IOException | IllegalStateException | XmlPullParserException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to load form field registry entries", e);
        }
    }

    private static int loadedFieldEntries;

    static int getLoadedFieldEntires() {
        return loadedFieldEntries;
    }

    private static void loadFormFieldRegistryEntry(InputStream inputStream, String source) throws XmlPullParserException, IOException {
        var parser = SmackXmlParser.newXmlParser(inputStream);
        if (parser.nextTag() != XmlPullParser.TagEvent.START_ELEMENT) throw new IllegalStateException();
        var elementName = parser.getName();
        if (!elementName.equals("form_type"))
            throw new IllegalStateException(
                            source + " does not start with 'form_type' element but with " + elementName);

        String formType = null;
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                var name = parser.getName();
                switch (name) {
                case "name":
                    formType = parser.nextText();
                    if (formType.isEmpty()) throw new IllegalStateException();
                    break;
                case "field":
                    var fieldName = parser.getAttributeValue("var");
                    var typeString = parser.getAttributeValue("type");
                    var type = FormField.Type.fromString(typeString);
                    if (formType == null) throw new IllegalStateException();

                    FormFieldRegistry.register(formType, fieldName, type);
                    loadedFieldEntries++;
                    LOGGER.finer("Registered " + fieldName + " for form " + formType + " with type " + type + " [" + source + "]");
                    break;
                }
                break;
            case END_DOCUMENT:
                break outerloop;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
    }
    private static void loadFormFieldRegistryEntries() throws IOException, IllegalStateException, XmlPullParserException, URISyntaxException {
        var url = XDataManager.class.getProtectionDomain().getCodeSource().getLocation();
        if (url == null) throw new IllegalStateException();

        if (url.getProtocol().equals("file") && !url.getPath().endsWith(".jar")) {
            var path = Paths.get(url.toURI());
            if (!Files.isDirectory(path)) throw new IllegalStateException("Code source location " + url + " is not a directory");
            var prefix = Paths.get(path.toString(),  "org.igniterealtime.smack", "xdata", "form-registry").toString();

            try (var walk = Files.walk(path)) {
                var files = walk.filter(Files::isRegularFile)
                                .filter(f -> f.toString().startsWith(prefix))
                                .collect(Collectors.toList());
                for (var file : files) {
                    var inputStream = Files.newInputStream(file);
                    try {
                        loadFormFieldRegistryEntry(inputStream, file.toString());
                    } finally {
                        inputStream.close();
                    }
                }
            }
            return;
        }

        if (!url.toString().endsWith(".jar")) throw new IllegalStateException("Code source location " + url + " is not a jar");
        try (var jar = new JarFile(url.getFile())) {
            var files = jar.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().startsWith("org.igniterealtime.smack/xdata/form-registry/"))
                .collect(Collectors.toList());
            for (var file : files) {
                var inputStream = jar.getInputStream(file);
                try {
                    loadFormFieldRegistryEntry(inputStream, file.toString());
                } finally {
                    inputStream.close();
                }
            }
        }
    }

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
                    String message = "The field '" + fieldName + "' from form type '" + formType
                                    + "' was already registered with field type '" + previousType
                                    + "' while it is now seen with type '" + fieldType
                                    + "'. Form field types have to be unambigiously."
                                    + " XMPP uses a registry for form field tpes, scoped by the form type."
                                    + " You may find the correct value at https://xmpp.org/registrar/formtypes.html";
                    throw new IllegalArgumentException(message);
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
