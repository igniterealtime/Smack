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

public class TextSingleFormField extends AbstractSingleStringValueFormField {

    protected TextSingleFormField(Builder builder) {
        super(builder);
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder
                    extends AbstractSingleStringValueFormField.Builder<TextSingleFormField, TextSingleFormField.Builder> {

        private Builder(TextSingleFormField textSingleFormField) {
            super(textSingleFormField);
        }

        Builder(String fieldName, FormField.Type type) {
            super(fieldName, type);
        }

        @Override
        public TextSingleFormField build() {
            return new TextSingleFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }

}
