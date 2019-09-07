/**
 *
 * Copyright 2003-2007 Jive Software, 2019 Florian Schmaus.
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

package org.jivesoftware.smackx.xdata;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.FullyQualifiedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.util.XmppDateTime;

/**
 * Represents a field of a form. The field could be used to represent a question to complete,
 * a completed question or a data returned from a search. The exact interpretation of the field
 * depends on the context where the field is used.
 *
 * @author Gaston Dombiak
 */
public final class FormField implements FullyQualifiedElement {

    public static final String ELEMENT = "field";

    public static final String NAMESPACE = DataForm.NAMESPACE;

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The constant String "FORM_TYPE".
     */
    public static final String FORM_TYPE = "FORM_TYPE";

    /**
     * Form Field Types as defined in XEP-4 § 3.3.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0004.html#protocol-fieldtypes">XEP-4 § 3.3 Field Types</a>
     */
    public enum Type {

        /**
         * Boolean type. Can be 0 or 1, true or false, yes or no. Default value is 0.
         * <p>
         * Note that in XEP-4 this type is called 'boolean', but since that String is a restricted keyword in Java, it
         * is named 'bool' in Smack.
         * </p>
         */
        bool,

        /**
         * Fixed for putting in text to show sections, or just advertise your web site in the middle of the form.
         */
        fixed,

        /**
         * Is not given to the user at all, but returned with the questionnaire.
         */
        hidden,

        /**
         * multiple entries for JIDs.
         */
        jid_multi,

        /**
         * Jabber ID - choosing a JID from your roster, and entering one based on the rules for a JID.
         */
        jid_single,

        /**
         * Given a list of choices, pick one or more.
         */
        list_multi,

        /**
         * Given a list of choices, pick one.
         */
        list_single,

        /**
         * Multiple lines of text entry.
         */
        text_multi,

        /**
         * Instead of showing the user what they typed, you show ***** to protect it.
         */
        text_private,

        /**
         * Single line or word of text.
         */
        text_single,
        ;

        @Override
        public String toString() {
            switch (this) {
            case bool:
                return "boolean";
            default:
                return this.name().replace('_', '-');
            }
        }

        /**
         * Get a form field type from the given string. If <code>string</code> is null, then null will be returned.
         *
         * @param string the string to transform or null.
         * @return the type or null.
         */
        public static Type fromString(String string) {
            if (string == null) {
                return null;
            }
            switch (string) {
            case "boolean":
                return bool;
            default:
                string = string.replace('-', '_');
                return Type.valueOf(string);
            }
        }
    }

    /**
     * The field's name. Put as value in the 'var' attribute of &lt;field/&gt;.
     */
    private final String variable;

    private final String label;

    private final Type type;

    private final List<FormFieldChildElement> formFieldChildElements;

    private final MultiMap<QName, FormFieldChildElement> formFieldChildElementsMap;

    /*
     * The following four fields are cache values which are represented as child elements of </form> and hence also
     * appear in formFieldChildElements.
     */
    private final List<Option> options;
    private final List<CharSequence> values;
    private final String description;
    private final boolean required;

    private MultiMap<QName, FormFieldChildElement> createChildElementsMap() {
        MultiMap<QName, FormFieldChildElement> formFieldChildElementsMap = new MultiMap<>(formFieldChildElements.size());
        for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
            formFieldChildElementsMap.put(formFieldChildElement.getQName(), formFieldChildElement);
        }
        return formFieldChildElementsMap.asUnmodifiableMultiMap();
    }

    private FormField(String value) {
        variable = FORM_TYPE;
        type = Type.hidden;
        label = null;
        required = false;
        description = null;

        formFieldChildElements = Collections.singletonList(new Value(value));
        values = Collections.singletonList(value);
        options = Collections.emptyList();

        formFieldChildElementsMap = createChildElementsMap();
    }

    private FormField(Builder builder) {
        variable = builder.variable;
        label = builder.label;
        type = builder.type;
        if (builder.formFieldChildElements != null) {
            formFieldChildElements = Collections.unmodifiableList(builder.formFieldChildElements);
        } else {
            formFieldChildElements = Collections.emptyList();
        }

        if (variable == null && type != Type.fixed) {
            throw new IllegalArgumentException("The variable can only be null if the form is of type fixed");
        }

        String description = null;
        boolean requiredElementAsChild = false;
        ArrayList<Option> options = new ArrayList<>(formFieldChildElements.size());
        ArrayList<CharSequence> values = new ArrayList<>(formFieldChildElements.size());
        for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
            if (formFieldChildElement instanceof Value) {
                Value value = (Value) formFieldChildElement;
                values.add(value.getValue());
            } else if (formFieldChildElement instanceof Option) {
                Option option = (Option) formFieldChildElement;
                options.add(option);
            } else if (formFieldChildElement instanceof Description) {
                description = ((Description) formFieldChildElement).getDescription();
            // Required is a singleton instance.
            } else if (formFieldChildElement == Required.INSTANCE) {
                requiredElementAsChild = true;
            }
        }
        options.trimToSize();
        values.trimToSize();
        this.options = Collections.unmodifiableList(options);
        this.values = Collections.unmodifiableList(values);
        this.description = description;

        required = requiredElementAsChild;

        formFieldChildElementsMap = createChildElementsMap();
    }

    /**
     * Returns a description that provides extra clarification about the question. This information
     * could be presented to the user either in tool-tip, help button, or as a section of text
     * before the question.
     * <p>
     * If the question is of type FIXED then the description should remain empty.
     * </p>
     *
     * @return description that provides extra clarification about the question.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the label of the question which should give enough information to the user to
     * fill out the form.
     *
     * @return label of the question.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns a List of the available options that the user has in order to answer
     * the question.
     *
     * @return List of the available options.
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * Returns true if the question must be answered in order to complete the questionnaire.
     *
     * @return true if the question must be answered in order to complete the questionnaire.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns an indicative of the format for the data to answer.
     *
     * @return format for the data to answer.
     * @see Type
     */
    public Type getType() {
        if (type == null) {
            return Type.text_single;
        }
        return type;
    }

    /**
     * Returns a List of the default values of the question if the question is part
     * of a form to fill out. Otherwise, returns a List of the answered values of
     * the question.
     *
     * @return a List of the default values or answered values of the question.
     */
    public List<CharSequence> getValues() {
        synchronized (values) {
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }

    /**
     * Returns the values a String. Note that you should use {@link #getValues()} whenever possible instead of this
     * method.
     *
     * @return a list of Strings representing the values
     * @see #getValues()
     * @since 4.3
     */
    public List<String> getValuesAsString() {
        List<CharSequence> valuesAsCharSequence = getValues();
        List<String> res = new ArrayList<>(valuesAsCharSequence.size());
        for (CharSequence value : valuesAsCharSequence) {
            res.add(value.toString());
        }
        return res;
    }

    /**
     * Returns the first value of this form field or {@code null}.
     *
     * @return the first value or {@code null}
     * @since 4.3
     */
    public String getFirstValue() {
        CharSequence firstValue;

        synchronized (values) {
            if (values.isEmpty()) {
                return null;
            }
            firstValue = values.get(0);
        }

        return firstValue.toString();
    }

    /**
     * Parses the first value of this form field as XEP-0082 date/time format and returns a date instance or {@code null}.
     *
     * @return a Date instance representing the date/time information of the first value of this field.
     * @throws ParseException if parsing fails.
     * @since 4.3.0
     */
    public Date getFirstValueAsDate() throws ParseException {
        String valueString = getFirstValue();
        if (valueString == null) {
            return null;
        }
        return XmppDateTime.parseXEP0082Date(valueString);
    }

    /**
     * Returns the field's name, also known as the variable name in case this is an filled out answer form.
     * <p>
     * According to XEP-4 § 3.2 the variable name (the 'var' attribute)
     * "uniquely identifies the field in the context of the form" (if the field is not of type 'fixed', in which case
     * the field "MAY possess a 'var' attribute")
     * </p>
     *
     * @return the field's name.
     */
    public String getVariable() {
        return variable;
    }

    public FormFieldChildElement getFormFieldChildElement(QName qname) {
        return formFieldChildElementsMap.getFirst(qname);
    }

    public List<FormFieldChildElement> getFormFieldChildElements(QName qname) {
        return formFieldChildElementsMap.getAll(qname);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public QName getQName() {
        return QNAME;
    }

    public Builder buildAnswer() {
        return asBuilder().resetValues();
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder buf = new XmlStringBuilder(this, enclosingNamespace);
        // Add attributes
        buf.optAttribute("label", getLabel());
        buf.optAttribute("var", getVariable());
        // If no 'type' is specified, the default is "text-single";
        buf.attribute("type", getType(), Type.text_single);

        if (formFieldChildElements.isEmpty()) {
            buf.closeEmptyElement();
        } else {
            buf.rightAngleBracket();

            buf.append(formFieldChildElements);

            buf.closeElement(this);
        }
        return buf;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof FormField))
            return false;

        FormField other = (FormField) obj;

        return toXML().toString().equals(other.toXML().toString());
    }

    @Override
    public int hashCode() {
        return toXML().toString().hashCode();
    }

    public static FormField hiddenFormType(String value) {
        return new FormField(value);
    }

    public static Builder builder(String fieldName) {
        return builder().setFieldName(fieldName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String variable;

        private String label;

        private Type type;

        private List<FormFieldChildElement> formFieldChildElements;

        private boolean disallowType;
        private boolean disallowFurtherFormFieldChildElements;

        private Builder() {
        }

        private Builder(FormField formField) {
            variable = formField.variable;
            label = formField.label;
            type = formField.type;
            // Create a new modifiable list.
            formFieldChildElements = CollectionUtil.newListWith(formField.formFieldChildElements);
        }

        public Builder setFieldName(String fieldName) {
            return setVariable(fieldName);
        }

        public Builder setVariable(String variable) {
            this.variable = variable;
            return this;
        }

        /**
         * Sets an indicative of the format for the data to answer.
         *
         * @param type an indicative of the format for the data to answer.
         * @return a reference to this builder.
         * @see Type
         */
        public Builder setType(Type type) {
            final Type oldType = this.type;

            this.type = type;

            try {
                checkFormFieldChildElementsConsistency();
            } catch (IllegalArgumentException e) {
                this.type = oldType;
                throw e;
            }

            return this;
        }

        /**
         * Sets a description that provides extra clarification about the question. This information
         * could be presented to the user either in tool-tip, help button, or as a section of text
         * before the question.
         * <p>
         * If the question is of type FIXED then the description should remain empty.
         * </p>
         *
         * @param description provides extra clarification about the question.
         * @return a reference to this builder.
         */
        public Builder setDescription(String description) {
            Description descriptionElement = new Description(description);
            setOnlyElement(descriptionElement, Description.class);
            return this;
        }

        /**
         * Sets the label of the question which should give enough information to the user to
         * fill out the form.
         *
         * @param label the label of the question.
         * @return a reference to this builder.
         */
        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets if the question must be answered in order to complete the questionnaire.
         *
         * @param required if the question must be answered in order to complete the questionnaire.
         * @return a reference to this builder.
         */
        public Builder setRequired(boolean required) {
            setOnlyElement(Required.INSTANCE, Required.class);
            return this;
        }

        /**
         * Adds a default value to the question if the question is part of a form to fill out.
         * Otherwise, adds an answered value to the question.
         *
         * @param value a default value or an answered value of the question.
         * @return a reference to this builder.
         */
        public Builder addValue(CharSequence value) {
            return addFormFieldChildElement(new Value(value));
        }

        /**
         * Adds the given Date as XEP-0082 formated string by invoking {@link #addValue(CharSequence)} after the date
         * instance was formated.
         *
         * @param date the date instance to add as XEP-0082 formated string.
         * @return a reference to this builder.
         */
        public Builder addValue(Date date) {
            String dateString = XmppDateTime.formatXEP0082Date(date);
            return addValue(dateString);
        }

        /**
         * Adds a default values to the question if the question is part of a form to fill out.
         * Otherwise, adds an answered values to the question.
         *
         * @param values default values or an answered values of the question.
         * @return a reference to this builder.
         */
        public Builder addValues(Collection<? extends CharSequence> values) {
            for (CharSequence value : values) {
                addValue(value);
            }
            return this;
        }

        public Builder addOption(String option) {
            return addOption(new Option(option));
        }

        /**
         * Adds an available options to the question that the user has in order to answer
         * the question.
         *
         * @param option a new available option for the question.
         * @return a reference to this builder.
         */
        public Builder addOption(Option option) {
            return addFormFieldChildElement(option);
        }

        public Builder addFormFieldChildElement(FormFieldChildElement formFieldChildElement) {
            if (disallowFurtherFormFieldChildElements) {
                throw new IllegalArgumentException();
            }

            if (formFieldChildElement.requiresNoTypeSet() && type != null) {
                throw new IllegalArgumentException("Elements of type " + formFieldChildElement.getClass()
                                + " can only be added to form fields where no type is set");
            }

            ensureThatFormFieldChildElementsIsSet();

            if (!formFieldChildElements.isEmpty() && formFieldChildElement.isExclusiveElement()) {
                throw new IllegalArgumentException("Elements of type " + formFieldChildElement.getClass()
                                + " must be the only child elements of a form field.");
            }

            disallowType = disallowType || formFieldChildElement.requiresNoTypeSet();
            disallowFurtherFormFieldChildElements = formFieldChildElement.isExclusiveElement();

            formFieldChildElements.add(formFieldChildElement);

            return this;
        }

        public Builder resetValues() {
            if (formFieldChildElements == null) {
                return this;
            }

            // TODO: Use Java' stream API once Smack's minimum Android SDK level is 24 or higher.
            Iterator<FormFieldChildElement> it = formFieldChildElements.iterator();
            while (it.hasNext()) {
                FormFieldChildElement formFieldChildElement = it.next();
                if (formFieldChildElement instanceof Value) {
                    it.remove();
                }
            }

            disallowType = disallowFurtherFormFieldChildElements = false;

            return this;
        }

        public FormField build() {
            return new FormField(this);
        }

        public Type getType() {
            return type;
        }

        private void ensureThatFormFieldChildElementsIsSet() {
            if (formFieldChildElements == null) {
                formFieldChildElements = new ArrayList<>(4);
            }
        }

        private <E extends FormFieldChildElement> void setOnlyElement(E element, Class<E> elementClass) {
            ensureThatFormFieldChildElementsIsSet();
            for (int i = 0; i < formFieldChildElements.size(); i++) {
                if (formFieldChildElements.get(i).getClass().equals(elementClass)) {
                    formFieldChildElements.set(i, element);
                    return;
                }
            }

            formFieldChildElements.add(0, element);
        }

        private void checkFormFieldChildElementsConsistency() {
            if (formFieldChildElements == null) {
                return;
            }

            for (FormFieldChildElement formFiledChildElement : formFieldChildElements) {
                formFiledChildElement.checkConsistency(this);
            }
        }
    }

    /**
     * Marker class for the standard, as per XEP-0004, child elements of form fields.
     */
    private abstract static class StandardFormFieldChildElement implements FormFieldChildElement {
    }

    /**
     * Represents the available option of a given FormField.
     *
     * @author Gaston Dombiak
     */
    public static final class Option extends StandardFormFieldChildElement {

        public static final String ELEMENT = "option";

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String label;

        private final Value value;

        public Option(String value) {
            this(null, value);
        }

        public Option(String label, String value) {
            this.label = label;
            this.value = new Value(value);
        }

        public Option(String label, Value value) {
            this.label = label;
            this.value = value;
        }

        /**
         * Returns the label that represents the option.
         *
         * @return the label that represents the option.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the value of the option.
         *
         * @return the value of the option.
         */
        public Value getValue() {
            return value;
        }

        /**
         * Returns the string representation of the value of the option.
         *
         * @return the value of the option.
         */
        public String getValueString() {
            return value.value.toString();
        }

        @Override
        public String toString() {
            return getLabel();
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public QName getQName() {
            return QNAME;
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            // Add attribute
            xml.optAttribute("label", getLabel());
            xml.rightAngleBracket();

            // Add element
            xml.element("value", getValueString());

            xml.closeElement(this);
            return xml;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsUtil.equals(this, obj, (e, o) -> {
                e.append(value, o.value)
                 .append(label, o.label);
            });
        }

        private final HashCode.Cache hashCodeCache = new HashCode.Cache();

        @Override
        public int hashCode() {
            return hashCodeCache.getHashCode(c ->
                c.append(value)
                 .append(label)
            );
        }

    }

    public static class Description extends StandardFormFieldChildElement {

        public static final String ELEMENT = "desc";

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String description;

        public Description(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public QName getQName() {
            return QNAME;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.rightAngleBracket();
            xml.escape(description);
            xml.closeElement(this);
            return xml;
        }
    }

    public static final class Required extends StandardFormFieldChildElement {

        public static final Required INSTANCE = new Required();

        public static final String ELEMENT = "required";

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private Required() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public QName getQName() {
            return QNAME;
        }

        @Override
        public boolean mustBeOnlyOfHisKind() {
            return true;
        }

        @Override
        public String toXML(XmlEnvironment xmlEnvironment) {
            return '<' + ELEMENT + "/>";
        }
    }

    public static class Value extends StandardFormFieldChildElement {

        public static final String ELEMENT = "value";

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final CharSequence value;

        public Value(CharSequence value) {
            this.value = value;
        }

        public CharSequence getValue() {
            return value;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public QName getQName() {
            return QNAME;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.rightAngleBracket();
            xml.escape(value);
            return xml.closeElement(this);
        }

        @Override
        public boolean equals(Object other) {
            return EqualsUtil.equals(this, other, (e, o) -> e.append(this.value, o.value));
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
