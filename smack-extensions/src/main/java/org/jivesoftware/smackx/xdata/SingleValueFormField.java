/*
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

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.util.CollectionUtil;

public abstract class SingleValueFormField extends FormField {

    private final Value rawValue;

    protected SingleValueFormField(Builder<?, ?> builder) {
        super(builder);
        rawValue = builder.rawValue;
    }

    @Override
    public final List<CharSequence> getValues() {
        CharSequence value = getValue();
        return CollectionUtil.emptyOrSingletonListFrom(value);
    }

    public abstract CharSequence getValue();

    public final Value getRawValue() {
        return rawValue;
    }

    @Override
    public final List<Value> getRawValues() {
        Value rawValue = getRawValue();
        return CollectionUtil.emptyOrSingletonListFrom(rawValue);
    }

    @Override
    protected void populateExtraXmlChildElements() {
        if (rawValue == null) {
            return;
        }

        extraXmlChildElements = Collections.singletonList(rawValue);
    }

    public abstract static class Builder<F extends SingleValueFormField, B extends Builder<F, B>>
                    extends FormField.Builder<F, B> {

        protected Builder(String fieldName, Type type) {
            super(fieldName, type);
        }

        protected Builder(SingleValueFormField formField) {
            super(formField);
            rawValue = formField.getRawValue();
        }

        protected Value rawValue;

        @Override
        protected void resetInternal() {
            rawValue = null;
        };

    }
}
