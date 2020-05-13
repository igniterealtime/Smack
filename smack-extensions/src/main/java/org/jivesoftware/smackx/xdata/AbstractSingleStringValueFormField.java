/**
 *
 * Copyright 2020 Florian Schmaus.
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

import java.net.URL;
import java.util.Date;

import org.jxmpp.util.XmppDateTime;

public class AbstractSingleStringValueFormField extends SingleValueFormField {

    private final String value;

    protected AbstractSingleStringValueFormField(Builder<?, ?> builder) {
        super(builder);
        value = builder.value;
    }

    @Override
    public final String getValue() {
        return value;
    }

    public final Integer getValueAsInt() {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value);
    }

    public abstract static class Builder<F extends FormField, B extends FormField.Builder<F, B>> extends FormField.Builder<F, B> {

        private String value;

        protected Builder(AbstractSingleStringValueFormField abstractSingleFormField) {
            super(abstractSingleFormField);
            value = abstractSingleFormField.getValue();
        }

        protected Builder(String fieldName, FormField.Type type) {
            super(fieldName, type);
        }

        @Override
        protected void resetInternal() {
            value = null;
        }

        /**
         * Set the value.
         *
         * @param value the value to set.
         * @return a reference to this builder.
         * @deprecated use {@link #setValue(CharSequence)} instead.
         */
        @Deprecated
        // TODO: Remove in Smack 4.6.
        public B addValue(CharSequence value) {
            return setValue(value);
        }

        public B setValue(CharSequence value) {
            this.value = value.toString();
            return getThis();
        }

        public B setValue(Enum<?> value) {
            this.value = value.toString();
            return getThis();
        }

        public B setValue(int value) {
            this.value = Integer.toString(value);
            return getThis();
        }

        public B setValue(URL value) {
            return setValue(value.toString());
        }

        public B setValue(Date date) {
            String dateString = XmppDateTime.formatXEP0082Date(date);
            return setValue(dateString);
        }
    }
}
