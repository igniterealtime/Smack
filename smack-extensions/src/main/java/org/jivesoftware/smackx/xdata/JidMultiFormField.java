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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.util.CollectionUtil;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.util.JidUtil;

public final class JidMultiFormField extends FormField {

    private final List<Jid> values;

    private final List<String> rawValues;

    protected JidMultiFormField(Builder builder) {
        super(builder);
        values = CollectionUtil.cloneAndSeal(builder.values);
        rawValues = CollectionUtil.cloneAndSeal(builder.rawValues);
    }

    @Override
    public List<Jid> getValues() {
        return values;
    }

    @Override
    public List<String> getRawValues() {
        return rawValues;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends FormField.Builder<JidMultiFormField, JidMultiFormField.Builder> {
        private List<Jid> values;

        private List<String> rawValues;

        private Builder(JidMultiFormField jidMultiFormField) {
            super(jidMultiFormField);
            values = CollectionUtil.newListWith(jidMultiFormField.getValues());
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.jid_multi);
        }

        private void ensureValuesAreInitialized() {
            if (values == null) {
                values = new ArrayList<>();
                rawValues = new ArrayList<>();
            }
        }

        @Override
        protected void resetInternal() {
            values = null;
            rawValues = null;
        }

        public Builder addValue(Jid jid) {
            return addValue(jid, null);
        }

        public Builder addValue(Jid jid, String rawValue) {
            if (rawValue == null) {
                rawValue = jid.toString();
            }

            ensureValuesAreInitialized();

            values.add(jid);
            rawValues.add(rawValue);

            return this;
        }

        public Builder addValues(Collection<? extends Jid> jids) {
            ensureValuesAreInitialized();

            values.addAll(jids);
            rawValues.addAll(JidUtil.toStringList(jids));
            return this;
        }

        @Override
        public JidMultiFormField build() {
            return new JidMultiFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }


}
