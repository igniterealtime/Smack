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

import java.util.List;

public interface FormFieldWithOptions {

    /**
     * Returns a List of the available options that the user has in order to answer
     * the question.
     *
     * @return List of the available options.
     */
    List<FormField.Option> getOptions();

    public interface Builder<B extends FormField.Builder<?, ?>> {

        default B addOption(String option) {
            return addOption(new FormField.Option(option));
        }

        /**
         * Adds an available options to the question that the user has in order to answer
         * the question.
         *
         * @param option a new available option for the question.
         * @return a reference to this builder.
         */
        B addOption(FormField.Option option);

    }
}
