/*
 *
 * Copyright 2015-2019 Florian Schmaus
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
package org.igniterealtime.smack.inttest.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SmackIntegrationTest {

    boolean onlyDefaultConnectionType() default false;

    int connectionCount() default -1;

    /**
     * Unique identifier for a section (or paragraph) of the document referenced by {@link SpecificationReference},
     * such as '6.2.1'.
     *
     * @return a document section identifier
     */
    String section() default "";

    /**
     * A quotation of relevant text from the section referenced by {@link #section()}.
     *
     * @return human-readable text from the references document and section.
     */
    String quote() default "";
}
