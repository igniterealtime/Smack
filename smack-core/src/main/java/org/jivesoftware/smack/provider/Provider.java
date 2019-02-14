/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Smack provider are the parsers used to deserialize raw XMPP into the according Java {@link Element}s.
 * <p>
 * At any time when {@link #parse(XmlPullParser, int)} is invoked any type of exception can be thrown. If the parsed
 * element does not follow the specification, for example by putting a string where only integers are allowed, then a
 * {@link org.jivesoftware.smack.SmackException} should be thrown.
 * </p>
 *
 * @author Florian Schmaus
 * @param <E> the type of the resulting element.
 */
public abstract class Provider<E extends Element> {

    private final Class<E> elementClass;

    @SuppressWarnings("unchecked")
    protected Provider() {
        Type currentType = getClass().getGenericSuperclass();
        while (!(currentType instanceof ParameterizedType)) {
            Class<?> currentClass = (Class<?>) currentType;
            currentType = currentClass.getGenericSuperclass();
        }
        ParameterizedType parameterizedGenericSuperclass = (ParameterizedType) currentType;
        Type[] actualTypeArguments = parameterizedGenericSuperclass.getActualTypeArguments();
        Type elementType = actualTypeArguments[0];

        elementClass =  (Class<E>) elementType;
    }

    public final Class<E> getElementClass() {
        return elementClass;
    }

    public final E parse(XmlPullParser parser) throws IOException, XmlPullParserException, SmackParsingException {
        // XPP3 calling convention assert: Parser should be at start tag
        ParserUtils.assertAtStartTag(parser);

        final int initialDepth = parser.getDepth();
        E e = parse(parser, initialDepth);

        // XPP3 calling convention assert: Parser should be at end tag of the consumed/parsed element
        ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
        return e;
    }

    public abstract E parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackParsingException;
}
