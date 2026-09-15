/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.fast;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public final class FastToken implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String token;
    private final String mechanism;
    private final Instant expiry;
    private final long count;

    public FastToken(String token, String mechanism) {
        this(token, mechanism, null, 0L);
    }

    public FastToken(String token, String mechanism, Instant expiry) {
        this(token, mechanism, expiry, 0L);
    }

    public FastToken(String token, String mechanism, Instant expiry, long count) {
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.mechanism = Objects.requireNonNull(mechanism, "mechanism must not be null");
        this.expiry = expiry;
        this.count = count;
    }

    public String getToken() {
        return token;
    }

    public String getMechanism() {
        return mechanism;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public long getCount() {
        return count;
    }

    public boolean isExpired() {
        return expiry != null && expiry.isBefore(Instant.now());
    }

    public FastToken withIncrementedCount() {
        return new FastToken(token, mechanism, expiry, count + 1);
    }

    public FastToken withCount(long newCount) {
        return new FastToken(token, mechanism, expiry, newCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastToken fastToken = (FastToken) o;
        return count == fastToken.count &&
                Objects.equals(token, fastToken.token) &&
                Objects.equals(mechanism, fastToken.mechanism) &&
                Objects.equals(expiry, fastToken.expiry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, mechanism, expiry, count);
    }

    @Override
    public String toString() {
        return "FastToken{" +
                "mechanism='" + mechanism + '\'' +
                ", expiry=" + expiry +
                ", count=" + count +
                '}';
    }
}
