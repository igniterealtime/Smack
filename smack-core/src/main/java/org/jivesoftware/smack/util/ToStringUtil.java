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
package org.jivesoftware.smack.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ToStringUtil {

    public static Builder builderFor(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append(clazz.getSimpleName()).append('(');
        return new Builder(sb);
    }

    public static final class Builder {
        private final StringBuilder sb;

        private Builder(StringBuilder sb) {
            this.sb = sb;
        }

        public Builder addValue(String name, Object value) {
            if (value == null) {
                return this;
            }
            if (sb.charAt(sb.length() - 1) != '(') {
                sb.append(' ');
            }
            sb.append(name).append("='").append(value).append('\'');
            return this;
        }

        public <V> Builder add(String name, Collection<? extends V> values, Function<?, V> toStringFunction) {
            if (values.isEmpty()) {
                return this;
            }

            sb.append(' ').append(name).append('[');

            List<String> stringValues = new ArrayList<>(values.size());
            for (V value : values) {
                String valueString = toStringFunction.apply(value).toString();
                stringValues.add(valueString);
            }

            StringUtils.appendTo(stringValues, ", ", sb);

            sb.append(']');

            return this;
        }

        public String build() {
            sb.append(')');

            return sb.toString();
        }
    }
}
