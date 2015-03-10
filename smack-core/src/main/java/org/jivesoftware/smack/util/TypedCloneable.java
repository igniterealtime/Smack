/**
 *
 * Copyright 2015 Florian Schmaus
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

/**
 * An extended version of {@link java.lang.Cloneable}, which defines a generic {@link #clone()}
 * method.
 *
 * @param <T> the type returned by {@link #clone()}.
 */
public interface TypedCloneable<T> extends Cloneable {

    /**
     * Clone this instance.
     *
     * @return a cloned version of this instance.
     */
    public T clone();

}
