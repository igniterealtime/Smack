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
package org.jivesoftware.smackx.xdata.form;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.xdata.AbstractMultiFormField;
import org.jivesoftware.smackx.xdata.AbstractSingleStringValueFormField;
import org.jivesoftware.smackx.xdata.BooleanFormField;
import org.jivesoftware.smackx.xdata.FormField;

import org.jxmpp.util.XmppDateTime;

public interface FormReader {

    FormField getField(String fieldName);

    default String readFirstValue(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return null;
        }
        return formField.getFirstValue();
    }

    default List<? extends CharSequence> readValues(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return Collections.emptyList();
        }
        return formField.getValues();
    }

    default List<String> readStringValues(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return Collections.emptyList();
        }
        AbstractMultiFormField multiFormField = formField.ifPossibleAs(AbstractMultiFormField.class);
        List<? extends CharSequence> charSequences =  multiFormField.getValues();
        return StringUtils.toStrings(charSequences);
    }

    default Boolean readBoolean(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return null;
        }
        BooleanFormField booleanFormField = formField.ifPossibleAs(BooleanFormField.class);
        return booleanFormField.getValueAsBoolean();
    }

    default Integer readInteger(String fieldName) {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return null;
        }
        AbstractSingleStringValueFormField textSingleFormField = formField.ifPossibleAs(AbstractSingleStringValueFormField.class);
        return textSingleFormField.getValueAsInt();
    }

    default Date readDate(String fieldName) throws ParseException {
        FormField formField = getField(fieldName);
        if (formField == null) {
            return null;
        }
        AbstractSingleStringValueFormField textSingleFormField = formField.ifPossibleAs(AbstractSingleStringValueFormField.class);
        String value = textSingleFormField.getValue();
        if (value == null) {
            return null;
        }
        return XmppDateTime.parseDate(value);
    }
}
