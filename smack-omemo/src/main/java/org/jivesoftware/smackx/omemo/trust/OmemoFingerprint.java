/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.trust;

public class OmemoFingerprint implements CharSequence {

    private final String fingerprintString;

    public OmemoFingerprint(String fingerprintString) {
        this.fingerprintString = fingerprintString;
    }

    @Override
    public int length() {
        return fingerprintString.length();
    }

    @Override
    public char charAt(int index) {
        return fingerprintString.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return fingerprintString.subSequence(start, end);
    }

    @Override
    public String toString() {
        return fingerprintString;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OmemoFingerprint)) {
            return false;
        }
        OmemoFingerprint otherFingerprint = (OmemoFingerprint) other;
        return this.toString().trim().equals(otherFingerprint.toString().trim());
    }

    /**
     * Split the fingerprint in blocks of 8 characters with spaces between.
     *
     * @return Block representation of the fingerprint.
     */
    public String blocksOf8Chars() {
        StringBuilder pretty = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i != 0) pretty.append(' ');
            pretty.append(this.fingerprintString.substring(8 * i, 8 * (i + 1)));
        }
        return pretty.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
