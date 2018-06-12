/**
 *
 * Copyright the original author or authors
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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Extends a {@link HashMap} with {@link WeakReference} values, so that
 * weak references which have been cleared are periodically removed from
 * the map. The cleaning occurs as part of {@link #put}, after a specific
 * number ({@link #cleanInterval}) of calls to {@link #put}.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 *
 * @author Boris Grozev
 */
public class CleaningWeakReferenceMap<K, V>
    extends HashMap<K, WeakReference<V>> {
    private static final long serialVersionUID = 0L;

    /**
     * The number of calls to {@link #put} after which to clean this map
     * (i.e. remove cleared {@link WeakReference}s from it).
     */
    private final int cleanInterval;

    /**
     * The number of times {@link #put} has been called on this instance
     * since the last time it was {@link #clean}ed.
     */
    private int numberOfInsertsSinceLastClean = 0;

    /**
     * Initializes a new {@link CleaningWeakReferenceMap} instance with the
     * default clean interval.
     */
    public CleaningWeakReferenceMap() {
        this(50);
    }

    /**
     * Initializes a new {@link CleaningWeakReferenceMap} instance with a given
     * clean interval.
     * @param cleanInterval the number of calls to {@link #put} after which the
     * map will clean itself.
     */
    public CleaningWeakReferenceMap(int cleanInterval) {
        this.cleanInterval = cleanInterval;
    }

    @Override
    public WeakReference<V> put(K key, WeakReference<V> value) {
        WeakReference<V> ret = super.put(key, value);

        if (numberOfInsertsSinceLastClean++ > cleanInterval) {
            numberOfInsertsSinceLastClean = 0;
            clean();
        }

        return ret;
    }

    /**
     * Removes all cleared entries from this map (i.e. entries whose value
     * is a cleared {@link WeakReference}).
     */
    private void clean() {
        Iterator<Entry<K, WeakReference<V>>> iter = entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, WeakReference<V>> e = iter.next();
            if (e != null && e.getValue() != null
                && e.getValue().get() == null) {
                iter.remove();
            }
        }
    }
}
