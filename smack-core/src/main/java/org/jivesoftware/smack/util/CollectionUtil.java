/**
 *
 * Copyright 2015-2018 Florian Schmaus
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CollectionUtil {

    public static <T> Collection<T> requireNotEmpty(Collection<T> collection, String collectionName) {
        if (collection == null) {
            throw new NullPointerException(collectionName + " must not be null.");
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(collectionName + " must not be empty.");
        }
        return collection;
    }

    public static <T, C extends Collection<T>> List<T> removeUntil(C collection, Predicate<T> predicate) {
        List<T> removedElements = new ArrayList<>(collection.size());
        for (Iterator<T> it = collection.iterator(); it.hasNext();) {
            T t = it.next();
            if (predicate.test(t)) {
                break;
            }
            removedElements.add(t);
            it.remove();
        }
        return removedElements;
    }

    public interface Predicate<T> {
        boolean test(T t);
    }
}
