/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.packet.id;

import org.jivesoftware.smack.util.StringUtils;

public final class RandomStringStanzaIdSource  {

    public static class Factory implements StanzaIdSourceFactory {

        private static final int REQUIRED_MIN_LENGTH = 10;

        private final int length;
        private final boolean verySecure;

        public static final Factory VERY_SECURE = new Factory(10, true);
        public static final Factory MEDIUM_SECURE = new Factory(10, false);

        public Factory(int length, boolean verySecure) {
            if (length < REQUIRED_MIN_LENGTH) {
                throw new IllegalArgumentException(
                                "Insufficient length " + length + ", must be at least " + REQUIRED_MIN_LENGTH);
            }
            this.length = length;
            this.verySecure = verySecure;
        }

        @Override
        public StanzaIdSource constructStanzaIdSource() {
            StanzaIdSource stanzaIdSource;
            if (verySecure) {
                stanzaIdSource = () -> StringUtils.randomString(length);
            } else {
                stanzaIdSource = () -> StringUtils.insecureRandomString(length);
            }
            return stanzaIdSource;
        }

    }
}
