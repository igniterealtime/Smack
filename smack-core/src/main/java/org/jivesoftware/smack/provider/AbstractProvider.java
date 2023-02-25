/**
 *
 * Copyright 2019-2021 Florian Schmaus
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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParserException;

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
        } else if (elementType instanceof TypeVariable) {
            TypeVariable<?> typeVariableElementType = (TypeVariable<?>) elementType;
            elementClass = (Class<E>) typeVariableElementType.getClass();
        } else {
            throw new AssertionError("Element type '" + elementType + "' (" + elementType.getClass()
                            + ") is neither of type Class, ParameterizedType or TypeVariable");
        }
    }

    public final Class<E> getElementClass() {
        return elementClass;
    }

    public static final class TextParseException extends SmackParsingException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private final ParseException parseException;

        private TextParseException(ParseException parseException) {
            super(parseException);
            this.parseException = parseException;
        }

        public ParseException getParseException() {
            return parseException;
        }
    }

    public static final class NumberFormatParseException extends SmackParsingException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private NumberFormatParseException(NumberFormatException numberFormatException) {
            super(numberFormatException);
        }
    }

    protected interface WrappableParser<E> {
        E parse() throws XmlPullParserException, IOException, SmackParsingException, ParseException;
    }

    protected static <E> E wrapExceptions(WrappableParser<E> parser)
                    throws XmlPullParserException, IOException, SmackParsingException {
        E e;
        try {
            e = parser.parse();
        } catch (ParseException parseException) {
            throw new TextParseException(parseException);
        } catch (NumberFormatException numberFormatException) {
            throw new NumberFormatParseException(numberFormatException);
        }

        return e;
    }
}
