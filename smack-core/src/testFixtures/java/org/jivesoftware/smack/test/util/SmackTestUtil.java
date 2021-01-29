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
package org.jivesoftware.smack.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.Provider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smack.xml.XmlPullParserFactory;
import org.jivesoftware.smack.xml.stax.StaxXmlPullParserFactory;
import org.jivesoftware.smack.xml.xpp3.Xpp3XmlPullParserFactory;

public class SmackTestUtil {

    @SuppressWarnings("ImmutableEnumChecker")
    public enum XmlPullParserKind {
        StAX(StaxXmlPullParserFactory.class),
        XPP3(Xpp3XmlPullParserFactory.class),
        ;

        public final XmlPullParserFactory factory;

        XmlPullParserKind(Class<? extends XmlPullParserFactory> factoryClass) {
            try {
                factory = factoryClass.getDeclaredConstructor().newInstance();
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new AssertionError(e);
            }
        }
    }

    public static <E extends Element, P extends Provider<E>> E parse(CharSequence xml, Class<P> providerClass, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        P provider = providerClassToProvider(providerClass);
        return parse(xml, provider, parserKind);
    }

    public static <E extends Element, P extends Provider<E>> E parse(InputStream inputStream, Class<P> providerClass, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        P provider = providerClassToProvider(providerClass);
        return parse(inputStream, provider, parserKind);
    }

    public static <E extends Element, P extends Provider<E>> E parse(Reader reader, Class<P> providerClass, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        P provider = providerClassToProvider(providerClass);
        return parse(reader, provider, parserKind);
    }

    public static <E extends Element> E parse(CharSequence xml, Provider<E> provider, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String xmlString = xml.toString();
        Reader reader = new StringReader(xmlString);
        return parse(reader, provider, parserKind);
    }

    public static <E extends Element> E parse(InputStream inputStream, Provider<E> provider, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return parse(inputStreamReader, provider, parserKind);
    }

    public static <E extends Element> E parse(Reader reader, Provider<E> provider, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        XmlPullParser parser = getParserFor(reader, parserKind);
        E element = provider.parse(parser);
        return element;
    }

    public static XmlPullParser getParserFor(String xml, XmlPullParserKind parserKind) throws XmlPullParserException, IOException {
        Reader reader = new StringReader(xml);
        return getParserFor(reader, parserKind);
    }

    public static XmlPullParser getParserFor(InputStream inputStream, XmlPullParserKind parserKind) throws XmlPullParserException, IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return getParserFor(inputStreamReader, parserKind);
    }

    public static XmlPullParser getParserFor(Reader reader, XmlPullParserKind parserKind) throws XmlPullParserException, IOException {
        XmlPullParser parser = parserKind.factory.newXmlPullParser(reader);
        forwardParserToStartElement(parser);
        return parser;
    }

    public static XmlPullParser getParserFor(String xml, QName startTagQName, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        XmlPullParser parser = getParserFor(xml, parserKind);
        forwardParserToStartElement(parser, p -> p.getQName().equals(startTagQName));
        return parser;
    }

    public static XmlPullParser getParserFor(String xml, String startTagLocalpart, XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException {
        XmlPullParser parser = getParserFor(xml, parserKind);
        forwardParserToStartElement(parser, p -> p.getName().equals(startTagLocalpart));
        return parser;
    }

    public static XmlPullParser createDummyParser() throws XmlPullParserException, IOException {
        String dummyElement = "<empty-element/>";
        return PacketParserUtils.getParserFor(dummyElement);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Element, P extends Provider<E>> P providerClassToProvider(Class<P> providerClass) {
        P provider;

        try {
            provider = (P) providerClass.getDeclaredField("INSTANCE").get(null);
            return provider;
        } catch (NoSuchFieldException e) {
            // Continue with the next approach.
        } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
            throw new AssertionError(e);
        }

        try {
            provider = providerClass.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
        }
        return provider;
    }

    private static void forwardParserToStartElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        forwardParserToStartElement(parser, p -> true);
    }

    private static void forwardParserToStartElement(XmlPullParser parser,
                    Predicate<XmlPullParser> doneForwarding) throws XmlPullParserException, IOException {
        outerloop: while (true) {
            XmlPullParser.Event event = parser.getEventType();
            switch (event) {
            case START_ELEMENT:
                if (doneForwarding.test(parser)) {
                    break outerloop;
                }
                break;
            case END_DOCUMENT:
                throw new IllegalArgumentException("Not matching START_ELEMENT found");
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
            parser.next();
        }
    }
}
