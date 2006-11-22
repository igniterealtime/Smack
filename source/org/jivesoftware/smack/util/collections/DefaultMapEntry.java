// GenericsNote: Converted.
/*
 *  Copyright 2001-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jivesoftware.smack.util.collections;


import java.util.Map;

/**
 * A restricted implementation of {@link java.util.Map.Entry} that prevents
 * the MapEntry contract from being broken.
 *
 * @author James Strachan
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Matt Hall, John Watkinson, Stephen Colebourne
 * @version $Revision: 1.1 $ $Date: 2005/10/11 17:05:32 $
 * @since Commons Collections 3.0
 */
public final class DefaultMapEntry <K,V> extends AbstractMapEntry<K, V> {

    /**
     * Constructs a new entry with the specified key and given value.
     *
     * @param key   the key for the entry, may be null
     * @param value the value for the entry, may be null
     */
    public DefaultMapEntry(final K key, final V value) {
        super(key, value);
    }

    /**
     * Constructs a new entry from the specified KeyValue.
     *
     * @param pair the pair to copy, must not be null
     * @throws NullPointerException if the entry is null
     */
    public DefaultMapEntry(final KeyValue<K, V> pair) {
        super(pair.getKey(), pair.getValue());
    }

    /**
     * Constructs a new entry from the specified MapEntry.
     *
     * @param entry the entry to copy, must not be null
     * @throws NullPointerException if the entry is null
     */
    public DefaultMapEntry(final Map.Entry<K, V> entry) {
        super(entry.getKey(), entry.getValue());
    }

}
