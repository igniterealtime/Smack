/**
 *
 * Copyright 2020-2021 Florian Schmaus.
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

import org.jivesoftware.smack.util.ParserUtils;

public class BooleanFormField extends SingleValueFormField {

    private final Boolean value;

    protected BooleanFormField(Builder builder) {
        super(builder);
        value = builder.value;
    }

    @Override
    public String getValue() {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public Boolean getValueAsBoolean() {
        return value;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends SingleValueFormField.Builder<BooleanFormField, BooleanFormField.Builder> {
        private Boolean value;

        private Builder(BooleanFormField booleanFormField) {
            super(booleanFormField);
            value = booleanFormField.value;
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.bool);
        }

        @Override
        protected void resetInternal() {
            super.resetInternal();
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
        public Builder addValue(CharSequence value) {
            return setValue(value);
        }

        public Builder setValue(CharSequence value) {
            rawValue = value.toString();
            boolean valueBoolean = ParserUtils.parseXmlBoolean(rawValue);
            return setValue(valueBoolean);
        }

        public Builder setValue(boolean value) {
            rawValue = Boolean.toString(value);
            this.value = value;
            return this;
        }

        @Override
        public BooleanFormField build() {
            return new BooleanFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }

    }

}
