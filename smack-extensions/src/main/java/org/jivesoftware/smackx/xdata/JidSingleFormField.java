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

import org.jxmpp.jid.Jid;

public class JidSingleFormField extends SingleValueFormField {

    private final Jid value;

    protected JidSingleFormField(Builder builder) {
        super(builder);
        value = builder.value;
    }

    @Override
    public Jid getValue() {
        return value;
    }

    public Builder asBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends SingleValueFormField.Builder<JidSingleFormField, JidSingleFormField.Builder> {
        private Jid value;

        private Builder(JidSingleFormField jidSingleFormField) {
            super(jidSingleFormField);
            value = jidSingleFormField.getValue();
        }

        Builder(String fieldName) {
            super(fieldName, FormField.Type.jid_single);
        }

        @Override
        protected void resetInternal() {
            super.resetInternal();
            value = null;
        }

        public Builder setValue(Jid value) {
            return setValue(value, null);
        }

        public Builder setValue(Jid value, String rawValue) {
            this.value = value;
            if (rawValue != null) {
                this.rawValue = rawValue;
            } else {
                this.rawValue = value.toString();
            }
            return this;
        }

        @Override
        public JidSingleFormField build() {
            return new JidSingleFormField(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
