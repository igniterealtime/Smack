/**
 *
 * Copyright © 2014-2015 Florian Schmaus
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
import java.lang.reflect.InvocationTargetException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.ParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class IntrospectionProvider{

    // Unfortunately, we have to create two introspection providers, with the exactly the same code here

    public static abstract class IQIntrospectionProvider<I extends IQ> extends IQProvider<I> {
        private final Class<I> elementClass;

        protected IQIntrospectionProvider(Class<I> elementClass) {
            this.elementClass = elementClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public I parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                        SmackException {
            try {
                return (I) parseWithIntrospection(elementClass, parser, initialDepth);
            }
            catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
                throw new SmackException(e);
            }
        }
    }

    public static abstract class PacketExtensionIntrospectionProvider<PE extends ExtensionElement> extends ExtensionElementProvider<PE> {
        private final Class<PE> elementClass;

        protected PacketExtensionIntrospectionProvider(Class<PE> elementClass) {
            this.elementClass = elementClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public PE parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                        SmackException {
            try {
                return (PE) parseWithIntrospection(elementClass, parser, initialDepth);
            }
            catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
                throw new SmackException(e);
            }
        }
    }

    public static Object parseWithIntrospection(Class<?> objectClass,
                    XmlPullParser parser, final int initialDepth) throws NoSuchMethodException, SecurityException,
                    InstantiationException, IllegalAccessException, XmlPullParserException,
                    IOException, IllegalArgumentException, InvocationTargetException,
                    ClassNotFoundException {
        ParserUtils.assertAtStartTag(parser);
        Object object = objectClass.getConstructor().newInstance();
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                String stringValue = parser.nextText();
                Class<?> propertyType = object.getClass().getMethod(
                                "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1)).getReturnType();
                // Get the value of the property by converting it from a
                // String to the correct object type.
                Object value = decode(propertyType, stringValue);
                // Set the value of the bean.
                object.getClass().getMethod(
                                "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1),
                                propertyType).invoke(object, value);
                break;

            case  XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        ParserUtils.assertAtEndTag(parser);
        return object;
    }

    /**
     * Decodes a String into an object of the specified type. If the object
     * type is not supported, null will be returned.
     *
     * @param type the type of the property.
     * @param value the encode String value to decode.
     * @return the String value decoded into the specified type.
     * @throws ClassNotFoundException
     */
    private static Object decode(Class<?> type, String value) throws ClassNotFoundException {
        String name = type.getName();
        switch (name) {
        case "java.lang.String":
            return value;
        case "boolean":
            return Boolean.valueOf(value);
        case "int":
            return Integer.valueOf(value);
        case "long":
            return Long.valueOf(value);
        case "float":
            return Float.valueOf(value);
        case "double":
            return Double.valueOf(value);
        case "short":
            return Short.valueOf(value);
        case "byte":
            return Byte.valueOf(value);
        case "java.lang.Class":
            return Class.forName(value);
        }
        return null;
    }
}
