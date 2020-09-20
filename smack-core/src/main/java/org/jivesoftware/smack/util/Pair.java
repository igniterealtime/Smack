/**
 *
 * Copyright 2020 Florian Schmaus.
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

public final class Pair<F, S> {

    private final F first;
    private final S second;

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F extends Object, S extends Object> Pair<F, S> create(F first, S second) {
        return new Pair<>(first, second);
    }

    public static <F extends Object, S extends Object> Pair<F, S> createAndInitHashCode(F first, S second) {
        Pair<F, S> pair = new Pair<>(first, second);
        pair.hashCode();
        return pair;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    private final HashCode.Cache hashCodeCache = new HashCode.Cache();

    @Override
    public int hashCode() {
        return hashCodeCache.getHashCode(c ->
            c.append(first)
             .append(second)
        );
    }

    @Override
    public boolean equals(Object object) {
        return EqualsUtil.equals(this, object, (e, o) ->
            e.append(first, o.first)
             .append(second, o.second)
        );
    }
}
