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

public final class EqualsUtil {

    private EqualsUtil() {
    }

    public static <T> boolean equals(T thisObject, Object other, EqualsComperator<T> equalsComperator) {
        if (other == null) {
            return false;
        }
        if (thisObject == other) {
            return true;
        }
        @SuppressWarnings("unchecked")
        Class<T> thisObjectClass = (Class<T>) thisObject.getClass();
        if (thisObjectClass != other.getClass()) {
            return false;
        }

        EqualsUtil.Builder equalsBuilder = new EqualsUtil.Builder();

        equalsComperator.compare(equalsBuilder, thisObjectClass.cast(other));

        return equalsBuilder.isEquals;
    }

    @FunctionalInterface
    public interface EqualsComperator<T> {
        void compare(EqualsUtil.Builder equalsBuilder, T other);
    }

    public static final class Builder {
        private boolean isEquals = true;

        private Builder() {
        }

        private void nullSafeCompare(Object left, Object right, Runnable runnable) {
            if (!isEquals) {
                return;
            }

            if (left == right) {
                return;
            }

            if (left == null || right == null) {
                isEquals = false;
                return;
            }

            runnable.run();
        }

        public <O> Builder append(O left, O right) {
            if (!isEquals) {
                return this;
            }

            if (left == right) {
                return this;
            }

            if (left == null || right == null) {
                isEquals = false;
                return this;
            }

            isEquals = left.equals(right);
            return this;
        }

        public Builder append(boolean left, boolean right) {
            if (!isEquals) {
                return this;
            }

            isEquals = left == right;
            return this;
        }

        public Builder append(boolean[] left, boolean[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(byte left, byte right) {
            if (!isEquals) {
                return this;
            }

            isEquals = left == right;
            return this;
        }

        public Builder append(byte[] left, byte[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(char left, char right) {
            if (!isEquals) {
                return this;
            }

            isEquals = left == right;
            return this;
        }

        public Builder append(char[] left, char[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(double left, double right) {
            if (!isEquals) {
                return this;
            }

            return append(Double.doubleToLongBits(left), Double.doubleToLongBits(right));
        }

        public Builder append(double[] left, double[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(float left, float right) {
            if (!isEquals) {
                return this;
            }

            return append(Float.floatToIntBits(left), Float.floatToIntBits(right));
        }

        public Builder append(float[] left, float[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(int left, int right) {
            if (!isEquals) {
                return this;
            }

            isEquals = left == right;
            return this;
        }

        public Builder append(int[] left, int[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }

        public Builder append(long left, long right) {
            if (!isEquals) {
                return this;
            }

            isEquals = left == right;
            return this;
        }

        public Builder append(long[] left, long[] right) {
            nullSafeCompare(left, right, () -> {
                if (left.length != right.length) {
                    isEquals = false;
                    return;
                }
                for (int i = 0; i < left.length && isEquals; i++) {
                    append(left[i], right[i]);
                }
            });
            return this;
        }
    }

}
