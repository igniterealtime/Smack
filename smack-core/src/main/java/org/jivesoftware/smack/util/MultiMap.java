/**
 *
 * Copyright © 2015-2019 Florian Schmaus
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A lightweight implementation of a MultiMap, that is a Map that is able to hold multiple values for every key.
 * <p>
 * This MultiMap uses a {@link LinkedHashMap} together with a {@link ArrayList} in order to keep the order of its entries.
 * </p>
 *
 * @param <K> the type of the keys the map uses.
 * @param <V> the type of the values the map uses.
 */
public class MultiMap<K, V> implements TypedCloneable<MultiMap<K, V>> {

    /**
     * The constant value {@value}.
     */
    public static final int DEFAULT_MAP_SIZE = 6;

    private static final int ENTRY_LIST_SIZE = 3;

    private final Map<K, List<V>> map;

    /**
     * Constructs a new MultiMap with a initial capacity of {@link #DEFAULT_MAP_SIZE}.
     */
    public MultiMap() {
        this(DEFAULT_MAP_SIZE);
    }

    /**
     * Constructs a new MultiMap.
     *
     * @param size the initial capacity.
     */
    public MultiMap(int size) {
        this(new LinkedHashMap<>(size));
    }

    private MultiMap(Map<K, List<V>> map) {
        this.map = map;
    }

    public int size() {
        int size = 0;
        for (List<V> list : map.values()) {
            size += list.size();
        }
        return size;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public boolean containsValue(V value) {
        for (List<V> list : map.values()) {
            if (list.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the first value for the given key, or <code>null</code> if there are no entries.
     *
     * @param key TODO javadoc me please
     * @return the first value or null.
     */
    public V getFirst(K key) {
        List<V> res = getAll(key);
        if (res.isEmpty()) {
            return null;
        } else {
            return res.iterator().next();
        }
    }

    /**
     * Get all values for the given key. Returns the empty set if there are none.
     * <p>
     * Changes to the returned set will update the underlying MultiMap if the return set is not empty.
     * </p>
     *
     * @param key TODO javadoc me please
     * @return all values for the given key.
     */
    public List<V> getAll(K key) {
        List<V> res = map.get(key);
        if (res == null) {
            res = Collections.emptyList();
        }
        return res;
    }

    public boolean put(K key, V value) {
        return putInternal(key, list -> list.add(value));
    }

    public boolean putFirst(K key, V value) {
        return putInternal(key, list -> list.add(0, value));
    }

    private boolean putInternal(K key, Consumer<List<V>> valueListConsumer) {
        boolean keyExisted;
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>(ENTRY_LIST_SIZE);
            map.put(key, list);
            keyExisted = false;
        } else {
            keyExisted = true;
        }

        valueListConsumer.accept(list);

        return keyExisted;
    }

    /**
     * Removes all mappings for the given key and returns the first value if there where mappings or <code>null</code> if not.
     *
     * @param key TODO javadoc me please
     * @return the first value of the given key or null.
     */
    public V remove(K key) {
        List<V> res = map.remove(key);
        if (res == null) {
            return null;
        }
        assert !res.isEmpty();
        return res.iterator().next();
    }

    /**
     * Remove the mapping of the given key to the value.
     * <p>
     * Returns <code>true</code> if the mapping was removed and <code>false</code> if the mapping did not exist.
     * </p>
     *
     * @param key TODO javadoc me please
     * @param value TODO javadoc me please
     * @return true if the mapping was removed, false otherwise.
     */
    public boolean removeOne(K key, V value) {
        List<V> list = map.get(key);
        if (list == null) {
            return false;
        }
        boolean res = list.remove(value);
        if (list.isEmpty()) {
            // Remove the key also if the value set is now empty
            map.remove(key);
        }
        return res;
    }

    /**
     * Remove the given number of values for a given key. May return less values then requested.
     *
     * @param key the key to remove from.
     * @param num the number of values to remove.
     * @return a list of the removed values.
     * @since 4.4.0
     */
    public List<V> remove(K key, int num) {
        List<V> values = map.get(key);
        if (values == null) {
            return Collections.emptyList();
        }

        final int resultSize = values.size() > num ? num : values.size();
        final List<V> result = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            result.add(values.get(0));
        }

        if (values.isEmpty()) {
            map.remove(key);
        }

        return result;
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        map.clear();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns a new list containing all values of this multi map.
     *
     * @return a new list with all values.
     */
    public List<V> values() {
        List<V> values = new ArrayList<>(size());
        for (List<V> list : map.values()) {
            values.addAll(list);
        }
        return values;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new LinkedHashSet<>(size());
        for (Map.Entry<K, List<V>> entries : map.entrySet()) {
            K key = entries.getKey();
            for (V value : entries.getValue()) {
                entrySet.add(new SimpleMapEntry<>(key, value));
            }
        }
        return entrySet;
    }

    public MultiMap<K, V> asUnmodifiableMultiMap() {
        LinkedHashMap<K, List<V>> mapCopy = new LinkedHashMap<>(map.size());
        for (Entry<K, List<V>> entry : map.entrySet()) {
            K key = entry.getKey();
            List<V> values = entry.getValue();

            mapCopy.put(key, Collections.unmodifiableList(values));
        }

        return new MultiMap<K, V>(Collections.unmodifiableMap(mapCopy));
    }

    @Override
    public MultiMap<K, V> clone() {
        Map<K, List<V>> clonedMap = new LinkedHashMap<>(map.size());

        // TODO: Use Map.forEach() once Smack's minimum Android API is 24 or higher.
        for (Entry<K, List<V>> entry : map.entrySet()) {
            List<V> clonedList = CollectionUtil.newListWith(entry.getValue());
            clonedMap.put(entry.getKey(), clonedList);
        }

        return new MultiMap<>(clonedMap);
    }

    private static final class SimpleMapEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private V value;

        private SimpleMapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V tmp = this.value;
            this.value = value;
            return tmp;
        }
    }

}
