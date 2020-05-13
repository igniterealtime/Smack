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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.util.CollectionUtil;

import org.jxmpp.util.XmppDateTime;

public class AbstractMultiFormField extends FormField {

    private final List<String> values;

    protected AbstractMultiFormField(Builder<?, ?> builder) {
        super(builder);
        values = CollectionUtil.cloneAndSeal(builder.values);
    }

    @Override
    public final List<String> getValues() {
        return values;
    }


    public abstract static class Builder<F extends FormField, B extends FormField.Builder<F, B>>
                    extends FormField.Builder<F, B> {

        private List<String> values;

        protected Builder(AbstractMultiFormField formField) {
            super(formField);
            values = CollectionUtil.newListWith(formField.getValues());
        }

        protected Builder(String fieldName, FormField.Type type) {
            super(fieldName, type);
        }

        private void ensureValuesAreInitialized() {
            if (values == null) {
                values = new ArrayList<>();
            }
        }

        @Override
        protected void resetInternal() {
            values = null;
        }

        public abstract B addValue(CharSequence value);

        public B addValueVerbatim(CharSequence value) {
            ensureValuesAreInitialized();

            values.add(value.toString());
            return getThis();
        }

        public final B addValue(Date date) {
            String dateString = XmppDateTime.formatXEP0082Date(date);
            return addValueVerbatim(dateString);
        }

        public final B addValues(Collection<? extends CharSequence> values) {
            ensureValuesAreInitialized();

            for (CharSequence value : values) {
                this.values.add(value.toString());
            }

            return getThis();
        }
    }
}
