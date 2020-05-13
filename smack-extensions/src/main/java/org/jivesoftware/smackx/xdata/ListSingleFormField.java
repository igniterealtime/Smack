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

public class ListSingleFormField  extends AbstractSingleStringValueFormField implements FormFieldWithOptions {

    private final List<Option> options;

    protected ListSingleFormField(Builder builder) {
        super(builder);
        options = CollectionUtil.cloneAndSeal(builder.options);
    }

    @Override
    public List<Option> getOptions() {
        return options;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    @Override
    protected void populateExtraXmlChildElements() {
        extraXmlChildElements = new ArrayList<>(1 + options.size());
        CharSequence value = getValue();
        if (value != null) {
            extraXmlChildElements.add(new FormField.Value(value));
        }

        extraXmlChildElements.addAll(options);
    }

    public static final class Builder
                    extends AbstractSingleStringValueFormField.Builder<ListSingleFormField, ListSingleFormField.Builder>
                    implements FormFieldWithOptions.Builder<Builder> {

        private List<Option> options;

        private Builder(ListSingleFormField textSingleFormField) {
            super(textSingleFormField);
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.list_single);
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
        public ListSingleFormField build() {
            return new ListSingleFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }

}
