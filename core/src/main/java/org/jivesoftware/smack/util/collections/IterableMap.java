// GenericsNote: Converted.
/*
 *  Copyright 2003-2004 The Apache Software Foundation
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
 * Defines a map that can be iterated directly without needing to create an entry set.
 * <p/>
 * A map iterator is an efficient way of iterating over maps.
 * There is no need to access the entry set or cast to Map Entry objects.
 * <pre>
 * IterableMap map = new HashedMap();
 * MapIterator it = map.mapIterator();
 * while (it.hasNext()) {
 *   Object key = it.next();
 *   Object value = it.getValue();
 *   it.setValue("newValue");
 * }
 * </pre>
 *
 * @author Matt Hall, John Watkinson, Stephen Colebourne
 * @version $Revision: 1.1 $ $Date: 2005/10/11 17:05:19 $
 * @since Commons Collections 3.0
 */
public interface IterableMap <K,V> extends Map<K, V> {

    /**
     * Obtains a <code>MapIterator</code> over the map.
     * <p/>
     * A map iterator is an efficient way of iterating over maps.
     * There is no need to access the entry set or cast to Map Entry objects.
     * <pre>
     * IterableMap map = new HashedMap();
     * MapIterator it = map.mapIterator();
     * while (it.hasNext()) {
     *   Object key = it.next();
     *   Object value = it.getValue();
     *   it.setValue("newValue");
     * }
     * </pre>
     *
     * @return a map iterator
     */
    MapIterator<K, V> mapIterator();

}
