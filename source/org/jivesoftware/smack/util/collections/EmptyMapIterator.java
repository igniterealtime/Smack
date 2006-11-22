// GenericsNote: Converted.
/*
 *  Copyright 2004 The Apache Software Foundation
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

/**
 * Provides an implementation of an empty map iterator.
 *
 * @author Matt Hall, John Watkinson, Stephen Colebourne
 * @version $Revision: 1.1 $ $Date: 2005/10/11 17:05:24 $
 * @since Commons Collections 3.1
 */
public class EmptyMapIterator extends AbstractEmptyIterator implements MapIterator, ResettableIterator {

    /**
     * Singleton instance of the iterator.
     *
     * @since Commons Collections 3.1
     */
    public static final MapIterator INSTANCE = new EmptyMapIterator();

    /**
     * Constructor.
     */
    protected EmptyMapIterator() {
        super();
    }

}
