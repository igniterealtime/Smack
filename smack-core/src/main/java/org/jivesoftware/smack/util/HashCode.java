/**
 *
 * Copyright 2019 Florian Schmaus.
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

public class HashCode {

    private static final int MULTIPLIER_VALUE = 37;

    public static class Cache {
        private boolean calculated;
        private int hashcode;

        public int getHashCode(Calculator hashCodeCalculator) {
            if (calculated) {
                return hashcode;
            }

            HashCode.Builder hashCodeBuilder = new HashCode.Builder();
            hashCodeCalculator.calculateHash(hashCodeBuilder);

            calculated = true;
            hashcode = hashCodeBuilder.hashcode;

            return hashcode;
        }

    }

    @FunctionalInterface
    public interface Calculator {
        void calculateHash(HashCode.Builder hashCodeBuilder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int hashcode = 17;

        private void applyHash() {
            applyHash(0);
        }

        private void applyHash(int hash) {
            hashcode = MULTIPLIER_VALUE * hashcode + hash;
        }

        public Builder append(Object object) {
            if (object == null) {
                applyHash();
                return this;
            }

            if (object.getClass().isArray()) {
                if (object instanceof int[]) {
                    append((int[]) object);
                } else if (object instanceof long[]) {
                    append((long[]) object);
                } else if (object instanceof boolean[]) {
                    append((boolean[]) object);
                } else if (object instanceof double[]) {
                    append((double[]) object);
                } else if (object instanceof float[]) {
                    append((float[]) object);
                } else if (object instanceof short[]) {
                    append((short[]) object);
                } else if (object instanceof char[]) {
                    append((char[]) object);
                } else if (object instanceof byte[]) {
                    append((byte[]) object);
                } else {
                    append((Object[]) object);
                }
            }
            applyHash(object.hashCode());
            return this;
        }

        public Builder append(boolean value) {
            applyHash(value ? 0 : 1);
            return this;
        }

        public Builder append(boolean[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (boolean bool : array) {
                append(bool);
            }
            return this;
        }

        public Builder append(byte value) {
            applyHash(value);
            return this;
        }

        public Builder append(byte[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (byte b : array) {
                append(b);
            }
            return this;
        }

        public Builder append(char value) {
            applyHash(value);
            return this;
        }

        public Builder append(char[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (char c : array) {
                append(c);
            }
            return this;
        }

        public Builder append(double value) {
            return append(Double.doubleToLongBits(value));
        }

        public Builder append(double[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (double d : array) {
                append(d);
            }
            return this;
        }

        public Builder append(float value) {
            return append(Float.floatToIntBits(value));
        }

        public Builder append(float[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (float f : array) {
                append(f);
            }
            return this;
        }

        public Builder append(long value) {
            applyHash((int) (value ^ (value >>> 32)));
            return this;
        }

        public Builder append(long[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (long l : array) {
                append(l);
            }
            return this;
        }

        public Builder append(Object[] array) {
            if (array == null) {
                applyHash();
                return this;
            }

            for (Object element : array) {
                append(element);
            }
            return this;
        }

        public int build() {
            return hashcode;
        }
    }

}
