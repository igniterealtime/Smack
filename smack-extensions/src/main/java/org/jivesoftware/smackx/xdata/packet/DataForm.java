/**
 *
 * Copyright 2003-2007 Jive Software, 2020 Florian Schmaus.
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

package org.jivesoftware.smackx.xdata.packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StanzaView;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.formtypes.FormFieldRegistry;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;

/**
 * Represents a form that could be use for gathering data as well as for reporting data
 * returned from a search.
 * <p>
 * Note that unlike many other things in XMPP, the order of the form fields is actually
 * Important in data forms.
 * </p>
 *
 * @author Gaston Dombiak
 */
public final class DataForm implements ExtensionElement {

    public static final String NAMESPACE = "jabber:x:data";
    public static final String ELEMENT = "x";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    public enum Type {
        /**
         * This stanza contains a form to fill out. Display it to the user (if your program can).
         */
        form,

        /**
         * The form is filled out, and this is the data that is being returned from the form.
         */
        submit,

        /**
         * The form was cancelled. Tell the asker that piece of information.
         */
        cancel,

        /**
         * Data results being returned from a search, or some other query.
         */
        result,
        ;

        public static Type fromString(String string) {
            return Type.valueOf(string.toLowerCase(Locale.US));
        }
    }

    private final Type type;
    private final String title;
    private final List<String> instructions;
    private final ReportedData reportedData;
    private final List<Item> items;
    private final List<FormField> fields;
    private final Map<String, FormField> fieldsMap;
    private final List<Element> extensionElements;

    private DataForm(Builder builder) {
        type = builder.type;
        title = builder.title;
        instructions = CollectionUtil.cloneAndSeal(builder.instructions);
        reportedData = builder.reportedData;
        items = CollectionUtil.cloneAndSeal(builder.items);
        fields = CollectionUtil.cloneAndSeal(builder.fields);
        fieldsMap = CollectionUtil.cloneAndSeal(builder.fieldsMap);
        extensionElements = CollectionUtil.cloneAndSeal(builder.extensionElements);

        // Ensure that the types of the form fields of every data form is known by registering such fields.
        if (type == Type.form) {
            FormFieldRegistry.register(this);
        }
    }

    /**
     * Returns the meaning of the data within the context. The data could be part of a form
     * to fill out, a form submission or data results.
     *
     * @return the form's type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the description of the data. It is similar to the title on a web page or an X
     * window.  You can put a &lt;title/&gt; on either a form to fill out, or a set of data results.
     *
     * @return description of the data.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns a List of the list of instructions that explain how to fill out the form and
     * what the form is about. The dataform could include multiple instructions since each
     * instruction could not contain newlines characters. Join the instructions together in order
     * to show them to the user.
     *
     * @return a List of the list of instructions that explain how to fill out the form.
     */
    public List<String> getInstructions() {
        return instructions;
    }

    /**
     * Returns the fields that will be returned from a search.
     *
     * @return fields that will be returned from a search.
     */
    public ReportedData getReportedData() {
        return reportedData;
    }

    /**
     * Returns a List of the items returned from a search.
     *
     * @return a List of the items returned from a search.
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Returns a List of the fields that are part of the form.
     *
     * @return a List of the fields that are part of the form.
     */
    public List<FormField> getFields() {
        return fields;
    }

    /**
     * Return the form field with the given variable name or null.
     *
     * @param fieldName the name of the field (the value of the 'var' (variable) attribute)
     * @return the form field or null.
     * @since 4.1
     */
    public FormField getField(String fieldName) {
        return fieldsMap.get(fieldName);
    }

    /**
     * Check if a form field with the given variable name exists.
     *
     * @param fieldName the name of the field.
     * @return true if a form field with the variable name exists, false otherwise.
     * @since 4.2
     */
    public boolean hasField(String fieldName) {
        return fieldsMap.containsKey(fieldName);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public List<Element> getExtensionElements() {
        return extensionElements;
    }

    /**
     * Return the form type from the hidden form type field.
     *
     * @return the form type or <code>null</code> if this form has none set.
     * @since 4.4.0
     */
    public String getFormType() {
        FormField formTypeField = getHiddenFormTypeField();
        if (formTypeField == null) {
            return null;
        }
        return formTypeField.getFirstValue();
    }

    /**
     * Returns the hidden FORM_TYPE field or null if this data form has none.
     *
     * @return the hidden FORM_TYPE field or null.
     * @since 4.1
     */
    public TextSingleFormField getHiddenFormTypeField() {
        FormField field = getField(FormField.FORM_TYPE);
        if (field == null) {
            return null;
        }
        return field.asHiddenFormTypeFieldIfPossible();
    }

    /**
     * Returns true if this DataForm has at least one FORM_TYPE field which is
     * hidden. This method is used for sanity checks.
     *
     * @return true if there is at least one field which is hidden.
     */
    public boolean hasHiddenFormTypeField() {
        return getHiddenFormTypeField() != null;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder buf = new XmlStringBuilder(this, xmlEnvironment);
        buf.attribute("type", getType());
        buf.rightAngleBracket();

        xmlEnvironment = buf.getXmlEnvironment();

        buf.optElement("title", getTitle());
        for (String instruction : getInstructions()) {
            buf.element("instructions", instruction);
        }
        // Append the list of fields returned from a search
        buf.optElement(getReportedData());
        // Loop through all the items returned from a search and append them to the string buffer
        buf.append(getItems());

        // Add all form fields.
        // We do not need to include the type for data forms of the type submit.
        boolean includeType = getType() != Type.submit;
        for (FormField formField : getFields()) {
            buf.append(formField.toXML(xmlEnvironment, includeType));
        }

        buf.append(getExtensionElements());
        buf.closeElement(this);
        return buf;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    /**
     * Get data form from a stanza.
     *
     * @param stanzaView the stanza to get data form from.
     * @return the DataForm or null
     */
    public static DataForm from(StanzaView stanzaView) {
        return from(stanzaView, null);
    }

    /**
     * Get the data form with the given form type from a stanza view.
     *
     * @param stanzaView the stanza view to retrieve the data form from
     * @param formType the form type
     * @return the retrieved data form or <code>null</code> if there is no matching one
     * @since 4.4.0
     */
    public static DataForm from(StanzaView stanzaView, String formType) {
        if (formType == null) {
            return stanzaView.getExtension(DataForm.class);
        }
        List<DataForm> dataForms = stanzaView.getExtensions(DataForm.class);
        return from(dataForms, formType);
    }

    /**
     * Return the first matching data form with the given form type from the given collection of data forms.
     *
     * @param dataForms the collection of data forms
     * @param formType the form type to match for
     * @return the first matching data form or <code>null</code> if there is none
     * @since 4.4.0
     */
    public static DataForm from(Collection<DataForm> dataForms, String formType) {
       for (DataForm dataForm : dataForms) {
           if (formType.equals(dataForm.getFormType())) {
               return dataForm;
           }
       }
       return null;
    }

    /**
     * Remove the first matching data form with the given form type from the given collection.
     *
     * @param dataForms the collection of data forms
     * @param formType the form type to match for
     * @return the removed data form or <code>null</code> if there was none removed
     * @since 4.4.0
     */
    public static DataForm remove(Collection<DataForm> dataForms, String formType) {
        Iterator<DataForm> it = dataForms.iterator();
        while (it.hasNext()) {
            DataForm dataForm = it.next();
            if (formType.equals(dataForm.getFormType())) {
                it.remove();
                return dataForm;
            }
        }
        return null;
    }

    /**
     * Get a new data form builder with the form type set to {@link Type#submit}.
     *
     * @return a new data form builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Type type) {
        return new Builder(type);
    }

    public static final class Builder {
        private Type type;
        private String title;
        private List<String> instructions;
        private ReportedData reportedData;
        private List<Item> items;
        private List<FormField> fields = new ArrayList<>();
        private Map<String, FormField> fieldsMap = new HashMap<>();
        private List<Element> extensionElements;

        private Builder() {
            this(Type.submit);
        }

        private Builder(Type type) {
            this.type = type;
        }

        private Builder(DataForm dataForm) {
            type = dataForm.getType();
            title = dataForm.getTitle();
            instructions = dataForm.getInstructions();
            reportedData = dataForm.getReportedData();
            items = CollectionUtil.newListWith(dataForm.getItems());
            fields = CollectionUtil.newListWith(dataForm.getFields());
            fieldsMap = new HashMap<>(dataForm.fieldsMap);
            extensionElements = CollectionUtil.newListWith(dataForm.getExtensionElements());
        }

        public Builder setType(Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Sets the description of the data. It is similar to the title on a web page or an X window.
         * You can put a &lt;title/&gt; on either a form to fill out, or a set of data results.
         *
         * @param title description of the data.
         * @return a reference to this builder.
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Adds a new field as part of the form.
         *
         * @param field the field to add to the form.
         * @return a reference to this builder.
         */
        public Builder addField(FormField field) {
            String fieldName = field.getFieldName();
            if (fieldName != null) {
                if (fieldsMap.containsKey(fieldName)) {
                    throw new IllegalArgumentException("A field with the name " + fieldName + " already exists");
                }

                fieldsMap.put(fieldName, field);
            }
            fields.add(field);

            return this;
        }

        /**
         * Add the given fields to this form.
         *
         * @param fieldsToAdd TODO javadoc me please
         * @return a reference to this builder.
         */
        public Builder addFields(Collection<? extends FormField> fieldsToAdd) {
            for (FormField field : fieldsToAdd) {
                String fieldName = field.getFieldName();
                if (fieldsMap.containsKey(fieldName)) {
                    throw new IllegalArgumentException("A field with the name " + fieldName + " already exists");
                }
            }
            for (FormField field : fieldsToAdd) {
                String fieldName = field.getFieldName();
                if (fieldName != null) {
                    fieldsMap.put(fieldName, field);
                }
                fields.add(field);
            }
            return this;
        }

        public Builder removeField(String fieldName) {
            FormField field = fieldsMap.remove(fieldName);
            if (field != null) {
                fields.remove(field);
            }
            return this;
        }

        public Builder setFormType(String formType) {
            FormField formField = FormField.buildHiddenFormType(formType);
            return addField(formField);
        }

        public Builder setInstructions(String instructions) {
            return setInstructions(StringUtils.splitLinesPortable(instructions));
        }

        /**
         * Sets the list of instructions that explain how to fill out the form and what the form is
         * about. The dataform could include multiple instructions since each instruction could not
         * contain newlines characters.
         *
         * @param instructions list of instructions that explain how to fill out the form.
         * @return a reference to this builder.
         */
        public Builder setInstructions(List<String> instructions) {
            this.instructions = instructions;
            return this;
        }

        /**
         * Adds a new instruction to the list of instructions that explain how to fill out the form
         * and what the form is about. The dataform could include multiple instructions since each
         * instruction could not contain newlines characters.
         *
         * @param instruction the new instruction that explain how to fill out the form.
         * @return a reference to this builder.
         */
        public Builder addInstruction(String instruction) {
            if (instructions == null) {
                instructions = new ArrayList<>();
            }
            instructions.add(instruction);
            return this;
        }

        /**
         * Adds a new item returned from a search.
         *
         * @param item the item returned from a search.
         * @return a reference to this builder.
         */
        public Builder addItem(Item item) {
            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(item);
            return this;
        }

        /**
         * Sets the fields that will be returned from a search.
         *
         * @param reportedData the fields that will be returned from a search.
         * @return a reference to this builder.
         */
        public Builder setReportedData(ReportedData reportedData) {
            this.reportedData = reportedData;
            return this;
        }

        public Builder addExtensionElement(Element element) {
            if (extensionElements == null) {
                extensionElements = new ArrayList<>();
            }
            extensionElements.add(element);
            return this;
        }

        public DataForm build() {
            return new DataForm(this);
        }
    }

    /**
     *
     * Represents the fields that will be returned from a search. This information is useful when
     * you try to use the jabber:iq:search namespace to return dynamic form information.
     *
     * @author Gaston Dombiak
     */
    public static class ReportedData implements ExtensionElement {
        public static final String ELEMENT = "reported";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final List<? extends FormField> fields;

        public ReportedData(List<? extends FormField> fields) {
            this.fields = Collections.unmodifiableList(fields);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        /**
         * Returns the fields returned from a search.
         *
         * @return the fields returned from a search.
         */
        public List<? extends FormField> getFields() {
            return fields;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.rightAngleBracket();
            xml.append(getFields());
            xml.closeElement(this);
            return xml;
        }

    }

    /**
     *
     * Represents items of reported data.
     *
     * @author Gaston Dombiak
     */
    public static class Item implements ExtensionElement {
        public static final String ELEMENT = "item";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final List<? extends FormField> fields;

        public Item(List<? extends FormField> fields) {
            this.fields = Collections.unmodifiableList(fields);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        /**
         * Returns the fields that define the data that goes with the item.
         *
         * @return the fields that define the data that goes with the item.
         */
        public List<? extends FormField> getFields() {
            return fields;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.rightAngleBracket();
            xml.append(getFields());
            xml.closeElement(this);
            return xml;
        }
    }
}
