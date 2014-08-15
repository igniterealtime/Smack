/**
 *
 * Copyright 2014 Vyacheslav Blinov
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

package org.jivesoftware.smackx.debugger.slf4j;

/**
 * This is package-level helper class to validate dependencies while initialization is in progress
 */
final class Validate {
    private Validate() { /* do not create instances */ }

    public static <T> T notNull(T instance) {
        return notNull(instance, null);
    }

    public static <T> T notNull(T instance, String message) {
        if (instance == null) {
            throw new NullPointerException(message);
        } else {
            return instance;
        }
    }
}
