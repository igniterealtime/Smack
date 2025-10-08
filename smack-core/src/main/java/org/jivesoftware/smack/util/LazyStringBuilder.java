/*
 *
 * Copyright 2014-2023 Florian Schmaus
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
import java.util.List;

public class LazyStringBuilder implements Appendable, CharSequence {

    private final List<CharSequence> list;

    private transient String cache;
    private int cachedLength = -1;

    private void invalidateCache() {
        cache = null;
        cachedLength = -1;
    }

    public LazyStringBuilder() {
        list = new ArrayList<>(20);
    }

    public LazyStringBuilder append(LazyStringBuilder lsb) {
        list.addAll(lsb.list);
        invalidateCache();
        return this;
    }

    @Override
    public LazyStringBuilder append(CharSequence csq) {
        assert csq != null;
        list.add(csq);
        invalidateCache();
        return this;
    }

    @Override
    public LazyStringBuilder append(CharSequence csq, int start, int end) {
        CharSequence subsequence = csq.subSequence(start, end);
        list.add(subsequence);
        invalidateCache();
        return this;
    }

    @Override
    public LazyStringBuilder append(char c) {
        list.add(Character.toString(c));
        invalidateCache();
        return this;
    }

    @Override
    public int length() {
        if (cachedLength >= 0) {
            return cachedLength;
        }

        int length = 0;
        try {
            for (CharSequence csq : list) {
                length += csq.length();
            }
        }
        catch (NullPointerException npe) {
            StringBuilder sb = safeToStringBuilder();
            throw new RuntimeException("The following LazyStringBuilder threw a NullPointerException:  " + sb, npe);
        }

        cachedLength = length;
        return length;
    }

    @Override
    public char charAt(int index) {
        if (cache != null) {
            return cache.charAt(index);
        }
        for (CharSequence csq : list) {
            if (index < csq.length()) {
                return csq.charAt(index);
            } else {
                index -= csq.length();
            }
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        if (cache == null) {
            StringBuilder sb = new StringBuilder(length());
            for (CharSequence csq : list) {
                sb.append(csq);
            }
            cache = sb.toString();
        }
        return cache;
    }

    public StringBuilder safeToStringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (CharSequence csq : list) {
            sb.append(csq);
        }
        return sb;
    }

    /**
     * Get the List of CharSequences representation of this instance. The list is unmodifiable. If
     * the resulting String was already cached, a list with a single String entry will be returned.
     *
     * @return a List of CharSequences representing this instance.
     */
    public List<CharSequence> getAsList() {
        if (cache != null) {
            return Collections.singletonList((CharSequence) cache);
        }
        return Collections.unmodifiableList(list);
    }
}
