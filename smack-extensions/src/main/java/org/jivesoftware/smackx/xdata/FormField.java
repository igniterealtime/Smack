/**
 *
 * Copyright 2003-2007 Jive Software.
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;

import org.jxmpp.util.XmppDateTime;

/**
 * Represents a field of a form. The field could be used to represent a question to complete,
 * a completed question or a data returned from a search. The exact interpretation of the field
 * depends on the context where the field is used.
 *
 * @author Gaston Dombiak
 */
public class FormField implements NamedElement {

    public static final String ELEMENT = "field";

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

    private final String variable;

    private String description;
    private boolean required = false;
    private String label;
    private Type type;
    private final List<Option> options = new ArrayList<>();
    private final List<CharSequence> values = new ArrayList<>();
    private ValidateElement validateElement;

    /**
     * Creates a new FormField with the variable name that uniquely identifies the field
     * in the context of the form.
     *
     * @param variable the variable name of the question.
     */
    public FormField(String variable) {
        this.variable = StringUtils.requireNotNullNorEmpty(variable, "Variable must not be null nor empty");
    }

    /**
     * Creates a new FormField of type FIXED. The fields of type FIXED do not define a variable
     * name.
     */
    public FormField() {
        this.variable = null;
        this.type = Type.fixed;
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
        synchronized (options) {
            return Collections.unmodifiableList(new ArrayList<>(options));
        }
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
     * Returns the variable name that the question is filling out.
     * <p>
     * According to XEP-4 § 3.2 the variable name (the 'var' attribute)
     * "uniquely identifies the field in the context of the form" (if the field is not of type 'fixed', in which case
     * the field "MAY possess a 'var' attribute")
     * </p>
     *
     * @return the variable name of the question.
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Get validate element.
     *
     * @return the validateElement
     */
    public ValidateElement getValidateElement() {
        return validateElement;
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
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the label of the question which should give enough information to the user to
     * fill out the form.
     *
     * @param label the label of the question.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets if the question must be answered in order to complete the questionnaire.
     *
     * @param required if the question must be answered in order to complete the questionnaire.
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Set validate element.
     * @param validateElement the validateElement to set
     */
    public void setValidateElement(ValidateElement validateElement) {
        validateElement.checkConsistency(this);
        this.validateElement = validateElement;
    }

    /**
     * Sets an indicative of the format for the data to answer.
     * <p>
     * This method will throw an IllegalArgumentException if type is 'fixed'. To create FormFields of type 'fixed' use
     * {@link #FormField()} instead.
     * </p>
     *
     * @param type an indicative of the format for the data to answer.
     * @see Type
     * @throws IllegalArgumentException if type is 'fixed'.
     */
    public void setType(Type type) {
        if (type == Type.fixed) {
            throw new IllegalArgumentException("Can not set type to fixed, use FormField constructor without arguments instead.");
        }
        this.type = type;
    }

    /**
     * Adds a default value to the question if the question is part of a form to fill out.
     * Otherwise, adds an answered value to the question.
     *
     * @param value a default value or an answered value of the question.
     */
    public void addValue(CharSequence value) {
        synchronized (values) {
            values.add(value);
        }
    }

    /**
     * Adds the given Date as XEP-0082 formated string by invoking {@link #addValue(CharSequence)} after the date
     * instance was formated.
     *
     * @param date the date instance to add as XEP-0082 formated string.
     * @since 4.3.0
     */
    public void addValue(Date date) {
        String dateString = XmppDateTime.formatXEP0082Date(date);
        addValue(dateString);
    }

    /**
     * Adds a default values to the question if the question is part of a form to fill out.
     * Otherwise, adds an answered values to the question.
     *
     * @param newValues default values or an answered values of the question.
     */
    public void addValues(List<? extends CharSequence> newValues) {
        synchronized (values) {
            values.addAll(newValues);
        }
    }

    /**
     * Removes all the values of the field.
     */
    protected void resetValues() {
        synchronized (values) {
            values.clear();
        }
    }

    /**
     * Adss an available options to the question that the user has in order to answer
     * the question.
     *
     * @param option a new available option for the question.
     */
    public void addOption(Option option) {
        synchronized (options) {
            options.add(option);
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder buf = new XmlStringBuilder(this);
        // Add attributes
        buf.optAttribute("label", getLabel());
        buf.optAttribute("var", getVariable());
        buf.optAttribute("type", getType());
        buf.rightAngleBracket();
        // Add elements
        buf.optElement("desc", getDescription());
        buf.condEmptyElement(isRequired(), "required");
        // Loop through all the values and append them to the string buffer
        for (CharSequence value : getValues()) {
            buf.element("value", value);
        }
        // Loop through all the values and append them to the string buffer
        for (Option option : getOptions()) {
            buf.append(option.toXML());
        }
        buf.optElement(validateElement);
        buf.closeElement(this);
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

    /**
     * Represents the available option of a given FormField.
     *
     * @author Gaston Dombiak
     */
    public static final class Option implements NamedElement {

        public static final String ELEMENT = "option";

        private final String value;
        private String label;

        public Option(String value) {
            this.value = value;
        }

        public Option(String label, String value) {
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
        public String getValue() {
            return value;
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
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            // Add attribute
            xml.optAttribute("label", getLabel());
            xml.rightAngleBracket();

            // Add element
            xml.element("value", getValue());

            xml.closeElement(this);
            return xml;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj.getClass() != getClass())
                return false;

            Option other = (Option) obj;

            if (!value.equals(other.value))
                return false;

            String thisLabel = label == null ? "" : label;
            String otherLabel = other.label == null ? "" : other.label;

            if (!thisLabel.equals(otherLabel))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 37 * result + value.hashCode();
            result = 37 * result + (label == null ? 0 : label.hashCode());
            return result;
        }
    }
}
