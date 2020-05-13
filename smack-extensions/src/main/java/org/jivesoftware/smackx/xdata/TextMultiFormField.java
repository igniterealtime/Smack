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

import java.util.List;

import org.jivesoftware.smack.util.StringUtils;

public class TextMultiFormField extends AbstractMultiFormField {

    protected TextMultiFormField(Builder builder) {
        super(builder);
    }

    public void addValuesWithNewlines(StringBuilder sb) {
        for (CharSequence value : getValues()) {
            sb.append(value);
        }
    }

    public StringBuilder getValueswithNewlines() {
        StringBuilder sb = new StringBuilder();
        addValuesWithNewlines(sb);
        return sb;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends AbstractMultiFormField.Builder<TextMultiFormField, TextMultiFormField.Builder> {

        private Builder(TextMultiFormField textMultiFormField) {
            super(textMultiFormField);
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.text_multi);
        }

        @Override
        public Builder addValue(CharSequence valueCharSequence) {
            String value = valueCharSequence.toString();
            List<String> lines = StringUtils.splitLinesPortable(value);
            return addValues(lines);
        }

        @Override
        public TextMultiFormField build() {
            return new TextMultiFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }

}
