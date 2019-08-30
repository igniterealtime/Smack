/**
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
package org.jivesoftware.smack.util;

import java.util.Collection;

public class Objects {

    /**
     * Checks that the specified object reference is not <code>null</code> and throws a customized
     * {@link IllegalArgumentException} if it is.
     * <p>
     * Note that unlike <code>java.util.Objects</code>, this method throws an {@link IllegalArgumentException} instead
     * of an {@link NullPointerException}.
     * </p>
     *
     * @param <T> the type of the reference.
     * @param obj the object reference to check for nullity.
     * @param message detail message to be used in the event that a {@link IllegalArgumentException} is thrown.
     * @return <code>obj</code> if not null.
     * @throws IllegalArgumentException in case <code>obj</code> is <code>null</code>.
     */
    public static <T> T requireNonNull(T obj, String message) throws IllegalArgumentException {
        if (obj == null) {
            if (message == null) {
                message = "Can not provide null argument";
            }
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    public static <T> T requireNonNull(T obj) {
        return requireNonNull(obj, null);
    }

    /**
     * Require a collection to be neither null, nor empty.
     *
     * @param collection collection
     * @param message error message
     * @param <T> Collection type
     * @return collection TODO javadoc me please
     */
    public static <T extends Collection<?>> T requireNonNullNorEmpty(T collection, String message) {
        if (requireNonNull(collection).isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
