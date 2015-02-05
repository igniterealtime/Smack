/**
 *
 * Copyright © 2015 Florian Schmaus
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
import java.util.Set;

/**
 * A lightweight implementation of a MultiMap, that is a Map that is able to hold multiple values for every key.
 * <p>
 * This MultiMap uses a LinkedHashMap together with a LinkedHashSet in order to keep the order of its entries.
 * </p>
 *
 * @param <K> the type of the keys the map uses.
 * @param <V> the type of the values the map uses.
 */
public class MultiMap<K,V> {

    /**
     * The constant value '6'.
     */
    public static final int DEFAULT_MAP_SIZE = 6;

    private static final int ENTRY_SET_SIZE = 3;

    private final Map<K, Set<V>> map;

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
        map = new LinkedHashMap<>(size);
    }

    public int size() {
        int size = 0;
        for (Set<V> set : map.values()) {
            size += set.size();
        }
        return size;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Set<V> set : map.values()) {
            if (set.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the first value for the given key, or <code>null</code> if there are no entries.
     *
     * @param key
     * @return the first value or null.
     */
    public V getFirst(Object key) {
        Set<V> res = getAll(key);
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
     * @param key
     * @return all values for the given key.
     */
    public Set<V> getAll(Object key) {
        Set<V> res = map.get(key);
        if (res == null) {
            res = Collections.emptySet();
        }
        return res;
    }

    public boolean put(K key, V value) {
        boolean keyExisted;
        Set<V> set = map.get(key);
        if (set == null) {
            set = new LinkedHashSet<>(ENTRY_SET_SIZE);
            set.add(value);
            map.put(key, set);
            keyExisted = false;
        } else {
            set.add(value);
            keyExisted = true;
        }
        return keyExisted;
    }

    /**
     * Removes all mappings for the given key and returns the first value if there where mappings or <code>null</code> if not.
     *
     * @param key
     * @return the first value of the given key or null.
     */
    public V remove(Object key) {
        Set<V> res = map.remove(key);
        if (res == null) {
            return null;
        }
        assert(!res.isEmpty());
        return res.iterator().next();
    }

    /**
     * Remove the mapping of the given key to the value.
     * <p>
     * Returns <code>true</code> if the mapping was removed and <code>false</code> if the mapping did not exist.
     * </p>
     *
     * @param key
     * @param value
     * @return true if the mapping was removed, false otherwise.
     */
    public boolean removeOne(Object key, V value) {
        Set<V> set = map.get(key);
        if (set == null) {
            return false;
        }
        boolean res = set.remove(value);
        if (set.isEmpty()) {
            // Remove the key also if the value set is now empty
            map.remove(key);
        }
        return res;
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

    public List<V> values() {
        List<V> values = new ArrayList<>(size());
        for (Set<V> set : map.values()) {
            values.addAll(set);
        }
        return values;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new LinkedHashSet<>(size());
        for (Map.Entry<K, Set<V>> entries : map.entrySet()) {
            K key = entries.getKey();
            for (V value : entries.getValue()) {
                entrySet.add(new SimpleMapEntry<>(key, value));
            }
        }
        return entrySet;
    }

    private static class SimpleMapEntry<K, V> implements Map.Entry<K, V> {

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
