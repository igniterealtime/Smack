/**
 *
 * Copyright 2003-2007 Jive Software, 2019-2021 Florian Schmaus.
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

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.util.XmppDateTime;

/**
 * Represents a field of a form. The field could be used to represent a question to complete,
 * a completed question or a data returned from a search. The exact interpretation of the field
 * depends on the context where the field is used.
 * <p>
 * Fields have a name, which is stored in the 'var' attribute of the field's XML representation.
 * Field instances of all types, except of type "fixed" must have a name.
 * </p>
 *
 * @author Gaston Dombiak
 */
public abstract class FormField implements XmlElement {

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
    private final String fieldName;

    private final String label;

    private final Type type;

    private final List<FormFieldChildElement> formFieldChildElements;

    private final MultiMap<QName, FormFieldChildElement> formFieldChildElementsMap;

    /*
     * The following four fields are cache values which are represented as child elements of </form> and hence also
     * appear in formFieldChildElements.
     */
    private final String description;
    private final boolean required;

    private MultiMap<QName, FormFieldChildElement> createChildElementsMap() {
        MultiMap<QName, FormFieldChildElement> formFieldChildElementsMap = new MultiMap<>(formFieldChildElements.size());
        for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
            formFieldChildElementsMap.put(formFieldChildElement.getQName(), formFieldChildElement);
        }
        return formFieldChildElementsMap.asUnmodifiableMultiMap();
    }

    protected FormField(Builder<?, ?> builder) {
        fieldName = builder.fieldName;
        label = builder.label;
        type = builder.type;
        if (builder.formFieldChildElements != null) {
            formFieldChildElements = Collections.unmodifiableList(builder.formFieldChildElements);
        } else {
            formFieldChildElements = Collections.emptyList();
        }

        if (fieldName == null && type != Type.fixed) {
            throw new IllegalArgumentException("The variable can only be null if the form is of type fixed");
        }

        String description = null;
        boolean requiredElementAsChild = false;
        ArrayList<CharSequence> values = new ArrayList<>(formFieldChildElements.size());
        for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
            if (formFieldChildElement instanceof Description) {
                description = ((Description) formFieldChildElement).getDescription();
            } else if (formFieldChildElement instanceof Required) {
                requiredElementAsChild = true;
            }
        }
        values.trimToSize();
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
    public List<? extends CharSequence> getValues() {
        return getRawValueCharSequences();
    }

    public abstract List<Value> getRawValues();

    private transient List<CharSequence> rawValueCharSequences;

    public final List<CharSequence> getRawValueCharSequences() {
        if (rawValueCharSequences == null) {
            List<Value> rawValues = getRawValues();
            rawValueCharSequences = new ArrayList<>(rawValues.size());
            for (Value value : rawValues) {
                rawValueCharSequences.add(value.value);
            }
        }

        return rawValueCharSequences;
    }

    public boolean hasValueSet() {
        List<?> values = getValues();
        return !values.isEmpty();
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
        List<? extends CharSequence> valuesAsCharSequence = getValues();
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
        List<? extends CharSequence> values = getValues();
        if (values.isEmpty()) {
            return null;
        }

        return values.get(0).toString();
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
     * @deprecated use {@link #getFieldName()} instead.
     */
    // TODO: Remove in Smack 4.5
    @Deprecated
    public String getVariable() {
        return getFieldName();
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
    public String getFieldName() {
        return fieldName;
    }

    public FormFieldChildElement getFormFieldChildElement(QName qname) {
        return formFieldChildElementsMap.getFirst(qname);
    }

    public List<FormFieldChildElement> getFormFieldChildElements(QName qname) {
        return formFieldChildElementsMap.getAll(qname);
    }

    public List<FormFieldChildElement> getFormFieldChildElements() {
        return formFieldChildElements;
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

    protected transient List<XmlElement> extraXmlChildElements;

    /**
     * Populate @{link {@link #extraXmlChildElements}}. Note that this method may be overridden by subclasses.
     */
    protected void populateExtraXmlChildElements() {
        List<Value> values = getRawValues();
        // Note that we need to create a new ArrayList here, since subclasses may add to it by overriding
        // populateExtraXmlChildElements.
        extraXmlChildElements = new ArrayList<>(values.size());
        extraXmlChildElements.addAll(values);
    }

    @Override
    public final XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        return toXML(enclosingNamespace, true);
    }

    public final XmlStringBuilder toXML(XmlEnvironment enclosingNamespace, boolean includeType) {
        XmlStringBuilder buf = new XmlStringBuilder(this, enclosingNamespace);
        // Add attributes
        buf.optAttribute("label", getLabel());
        buf.optAttribute("var", getFieldName());

        if (includeType) {
            // If no 'type' is specified, the default is "text-single";
            buf.attribute("type", getType(), Type.text_single);
        }

        if (extraXmlChildElements == null) {
            // If extraXmlChildElements is null, see if they should be populated.
            populateExtraXmlChildElements();
        }

        if (formFieldChildElements.isEmpty()
                        && (extraXmlChildElements == null || extraXmlChildElements.isEmpty())) {
            buf.closeEmptyElement();
        } else {
            buf.rightAngleBracket();

            buf.optAppend(extraXmlChildElements);
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

    public static BooleanFormField.Builder booleanBuilder(String fieldName) {
        return new BooleanFormField.Builder(fieldName);
    }

    public static TextSingleFormField.Builder fixedBuilder() {
        return fixedBuilder(null);
    }

    public static TextSingleFormField.Builder fixedBuilder(String fieldName) {
        return new TextSingleFormField.Builder(fieldName, Type.fixed);
    }

    public static TextSingleFormField.Builder hiddenBuilder(String fieldName) {
        return new TextSingleFormField.Builder(fieldName, Type.hidden);
    }

    public static JidMultiFormField.Builder jidMultiBuilder(String fieldName) {
        return new JidMultiFormField.Builder(fieldName);
    }

    public static JidSingleFormField.Builder jidSingleBuilder(String fieldName) {
        return new JidSingleFormField.Builder(fieldName);
    }

    public static ListMultiFormField.Builder listMultiBuilder(String fieldName) {
        return new ListMultiFormField.Builder(fieldName);
    }

    public static ListSingleFormField.Builder listSingleBuilder(String fieldName) {
        return new ListSingleFormField.Builder(fieldName);
    }

    public static TextMultiFormField.Builder textMultiBuilder(String fieldName) {
        return new TextMultiFormField.Builder(fieldName);
    }

    public static TextSingleFormField.Builder textPrivateBuilder(String fieldName) {
        return new TextSingleFormField.Builder(fieldName, Type.text_private);
    }

    public static TextSingleFormField.Builder textSingleBuilder(String fieldName) {
        return new TextSingleFormField.Builder(fieldName, Type.text_single);
    }

    public static TextSingleFormField.Builder builder(String fieldName) {
        return textSingleBuilder(fieldName);
    }

    public static TextSingleFormField buildHiddenFormType(String formType) {
        return hiddenBuilder(FORM_TYPE).setValue(formType).build();
    }

    public <F extends FormField> F ifPossibleAs(Class<F> formFieldClass) {
        if (formFieldClass.isInstance(this)) {
            return formFieldClass.cast(this);
        }
        return null;
    }

    public <F extends FormField> F ifPossibleAsOrThrow(Class<F> formFieldClass) {
        F field = ifPossibleAs(formFieldClass);
        if (field == null) {
            throw new IllegalArgumentException();
        }
        return field;
    }

    public TextSingleFormField asHiddenFormTypeFieldIfPossible() {
        TextSingleFormField textSingleFormField = ifPossibleAs(TextSingleFormField.class);
        if (textSingleFormField == null) {
            return null;
        }
        if (getType() != Type.hidden) {
            return null;
        }
        if (!getFieldName().equals(FORM_TYPE)) {
            return null;
        }
        return textSingleFormField;
    }

    public abstract static class Builder<F extends FormField, B extends Builder<?, ?>> {
        private final String fieldName;
        private final Type type;

        private String label;

        private List<FormFieldChildElement> formFieldChildElements;

        private boolean disallowType;
        private boolean disallowFurtherFormFieldChildElements;

        protected Builder(String fieldName, Type type) {
            if (StringUtils.isNullOrEmpty(fieldName) && type != Type.fixed) {
                throw new IllegalArgumentException("Fields of type " + type + " must have a field name set");
            }
            this.fieldName = fieldName;
            this.type = type;
        }

        protected Builder(FormField formField) {
            // TODO: Is this still correct?
            fieldName = formField.fieldName;
            label = formField.label;
            type = formField.type;
            // Create a new modifiable list.
            formFieldChildElements = CollectionUtil.newListWith(formField.formFieldChildElements);
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
        public B setDescription(String description) {
            Description descriptionElement = new Description(description);
            setOnlyElement(descriptionElement);
            return getThis();
        }

        /**
         * Sets the label of the question which should give enough information to the user to
         * fill out the form.
         *
         * @param label the label of the question.
         * @return a reference to this builder.
         */
        public B setLabel(String label) {
            this.label = StringUtils.requireNotNullNorEmpty(label, "label must not be null or empty");
            return getThis();
        }

        /**
         * Sets if the question must be answered in order to complete the questionnaire.
         *
         * @return a reference to this builder.
         */
        public B setRequired() {
            return setRequired(true);
        }

        /**
         * Sets if the question must be answered in order to complete the questionnaire.
         *
         * @param required if the question must be answered in order to complete the questionnaire.
         * @return a reference to this builder.
         */
        public B setRequired(boolean required) {
            if (required) {
                setOnlyElement(Required.INSTANCE);
            }
            return getThis();
        }

        public B addFormFieldChildElements(Collection<? extends FormFieldChildElement> formFieldChildElements) {
            for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
                addFormFieldChildElement(formFieldChildElement);
            }
            return getThis();
        }

        @SuppressWarnings("ModifyCollectionInEnhancedForLoop")
        public B addFormFieldChildElement(FormFieldChildElement newFormFieldChildElement) {
            if (disallowFurtherFormFieldChildElements) {
                throw new IllegalArgumentException();
            }

            if (newFormFieldChildElement.requiresNoTypeSet() && type != null) {
                throw new IllegalArgumentException("Elements of type " + newFormFieldChildElement.getClass()
                                + " can only be added to form fields where no type is set");
            }

            ensureThatFormFieldChildElementsIsSet();

            if (!formFieldChildElements.isEmpty() && newFormFieldChildElement.isExclusiveElement()) {
                throw new IllegalArgumentException("Elements of type " + newFormFieldChildElement.getClass()
                                + " must be the only child elements of a form field.");
            }

            disallowType = disallowType || newFormFieldChildElement.requiresNoTypeSet();
            disallowFurtherFormFieldChildElements = newFormFieldChildElement.isExclusiveElement();

            formFieldChildElements.add(newFormFieldChildElement);

            for (FormFieldChildElement formFieldChildElement : formFieldChildElements) {
                try {
                    formFieldChildElement.checkConsistency(this);
                } catch (IllegalArgumentException e) {
                    // Remove the newly added form field child element if there it causes inconsistency.
                    formFieldChildElements.remove(newFormFieldChildElement);
                    throw e;
                }
            }

            return getThis();
        }

        protected abstract void resetInternal();

        public B reset() {
            resetInternal();

            if (formFieldChildElements == null) {
                return getThis();
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

            return getThis();
        }

        public abstract F build();

        public Type getType() {
            return type;
        }

        private void ensureThatFormFieldChildElementsIsSet() {
            if (formFieldChildElements == null) {
                formFieldChildElements = new ArrayList<>(4);
            }
        }

        private <E extends FormFieldChildElement> void setOnlyElement(E element) {
            Class<?> elementClass = element.getClass();
            ensureThatFormFieldChildElementsIsSet();
            for (int i = 0; i < formFieldChildElements.size(); i++) {
                if (formFieldChildElements.get(i).getClass().equals(elementClass)) {
                    formFieldChildElements.set(i, element);
                    return;
                }
            }

            addFormFieldChildElement(element);
        }

        public abstract B getThis();
    }

    /**
     * Marker class for the standard, as per XEP-0004, child elements of form fields.
     */
    private abstract static class StandardFormFieldChildElement implements FormFieldChildElement {
    }

    /**
     * Represents the available options of a {@link ListSingleFormField} and {@link ListMultiFormField}.
     *
     * @author Gaston Dombiak
     */
    public static final class Option implements XmlElement {

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

    public static class Value implements XmlElement {

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
