/**
 *
 * Copyright 2020 Florian Schmaus
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.CollectionUtil;

public class ListMultiFormField extends AbstractMultiFormField implements FormFieldWithOptions {

    private final List<Option> options;

    protected ListMultiFormField(Builder builder) {
        super(builder);
        options = CollectionUtil.cloneAndSeal(builder.options);
    }

    @Override
    public List<Option> getOptions() {
        return options;
    }

    @Override
    protected void populateExtraXmlChildElements() {
        super.populateExtraXmlChildElements();
        extraXmlChildElements.addAll(options);
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder
                    extends AbstractMultiFormField.Builder<ListMultiFormField, ListMultiFormField.Builder>
                    implements FormFieldWithOptions.Builder<Builder> {

        private List<Option> options;

        private Builder(ListMultiFormField textMultiFormField) {
            super(textMultiFormField);
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.list_multi);
        }

        @Override
        public Builder addValue(CharSequence value) {
            return super.addValueVerbatim(value);
        }

        @Override
        public Builder addOption(Option option) {
            if (options == null) {
                options = new ArrayList<>();
            }
            options.add(option);
            return this;
        }

        @Override
        public ListMultiFormField build() {
            return new ListMultiFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }

    }

}
