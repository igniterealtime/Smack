/**
 *
 * Copyright 2019-2020 Florian Schmaus
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
package org.jivesoftware.smack.provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.jivesoftware.smack.packet.Element;

public class AbstractProvider<E extends Element> {

    private final Class<E> elementClass;

    @SuppressWarnings("unchecked")
    protected AbstractProvider() {
        Type currentType = getClass().getGenericSuperclass();
        while (!(currentType instanceof ParameterizedType)) {
            Class<?> currentClass = (Class<?>) currentType;
            currentType = currentClass.getGenericSuperclass();
        }
        ParameterizedType parameterizedGenericSuperclass = (ParameterizedType) currentType;
        Type[] actualTypeArguments = parameterizedGenericSuperclass.getActualTypeArguments();
        Type elementType = actualTypeArguments[0];


        if (elementType instanceof Class) {
            elementClass = (Class<E>) elementType;
        } else if (elementType instanceof ParameterizedType) {
            ParameterizedType parameteriezedElementType = (ParameterizedType) elementType;
            elementClass = (Class<E>) parameteriezedElementType.getRawType();
        } else {
            throw new AssertionError(
                            "Element type '" + elementType + "' is neither of type Class or ParameterizedType");
        }
    }

    public final Class<E> getElementClass() {
        return elementClass;
    }
}
