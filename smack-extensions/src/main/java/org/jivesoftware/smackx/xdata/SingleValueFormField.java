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

import java.util.Collections;
import java.util.List;

public abstract class SingleValueFormField extends FormField {

    protected SingleValueFormField(Builder<?, ?> builder) {
        super(builder);
    }

    @Override
    public final List<CharSequence> getValues() {
        CharSequence value = getValue();
        if (value == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(value);
    }

    public abstract CharSequence getValue();

    @Override
    protected void populateExtraXmlChildElements() {
        CharSequence value = getValue();
        if (value == null) {
            return;
        }

        extraXmlChildElements = Collections.singletonList(new Value(value));
    }
}
