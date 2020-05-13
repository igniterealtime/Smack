/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import org.jivesoftware.smack.packet.FullyQualifiedElement;

public interface FormFieldChildElement extends FullyQualifiedElement {

    default boolean isExclusiveElement() {
        return false;
    }

    default boolean requiresNoTypeSet() {
        return isExclusiveElement();
    }

    default boolean mustBeOnlyOfHisKind() {
        return false;
    }

    default void checkConsistency(FormField.Builder<?, ?> formFieldBuilder) throws IllegalArgumentException {
        // Does nothing per default.
    }

    default void validate(FormField formField) throws IllegalArgumentException {
        // Does nothing per default.
    }
}
