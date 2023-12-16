/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.file_metadata;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;
import org.jivesoftware.smackx.file_metadata.provider.FileMetadataElementProvider;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jxmpp.util.XmppDateTime;

public class FileMetadataElementTest extends SmackTestSuite {

    private static Date date;
    private static FileMetadataElement metadataElement;
    private static final String expectedXml = "<file xmlns='urn:xmpp:file:metadata:0'>" +
            "<date>2015-07-26T20:46:00.000+00:00</date>" +
            "<width>1920</width>" +
            "<height>1080</height>" +
            "<desc>Picture of 24th XSF Summit</desc>" +
            "<desc xml:lang='de'>Foto vom 24. XSF Summit</desc>" +
            "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU=</hash>" +
            "<length>63000</length>" +
            "<media-type>text/plain</media-type>" +
            "<name>text.txt</name>" +
            "<size>6144</size>" +
            "</file>";

    private static final String expectedLegacyXml = "<file xmlns='urn:xmpp:file:metadata:0'>" +
            "<date>2015-07-26T20:46:00.000+00:00</date>" +
            "<dimensions>1920x1080</dimensions>" +
            "<desc>Picture of 24th XSF Summit</desc>" +
            "<desc xml:lang='de'>Foto vom 24. XSF Summit</desc>" +
            "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU=</hash>" +
            "<length>63000</length>" +
            "<media-type>text/plain</media-type>" +
            "<name>text.txt</name>" +
            "<size>6144</size>" +
            "</file>";

    @BeforeAll
    public static void setup() throws ParseException {
        date = XmppDateTime.parseDate("2015-07-26T21:46:00+01:00");
        metadataElement = FileMetadataElement.builder()
                .setModificationDate(date)
                .setWidth(1920)
                .setHeight(1080)
                .addDescription("Picture of 24th XSF Summit")
                .addDescription("Foto vom 24. XSF Summit", "de")
                .addHash(new HashElement(HashManager.ALGORITHM.SHA_256, "2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU="))
                .setLength(63000)
                .setMediaType("text/plain")
                .setName("text.txt")
                .setSize(6144)
                .build();
    }


    @Test
    public void testSerialization() {
        assertXmlSimilar(expectedXml, metadataElement.toXML().toString());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        FileMetadataElement parsed = SmackTestUtil.parse(expectedXml, FileMetadataElementProvider.class, parserKind);

        assertEquals(metadataElement, parsed);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testLegacyParsing(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        FileMetadataElement parsed = SmackTestUtil.parse(expectedLegacyXml, FileMetadataElementProvider.class, parserKind);

        assertEquals(metadataElement, parsed);
    }

    @Test
    public void nameIsEscaped() {
        FileMetadataElement e = FileMetadataElement.builder().setName("/etc/passwd").build();
        assertEquals("%2Fetc%2Fpasswd", e.getName());
    }

    @Test
    public void rejectNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setSize(-1));
    }

    @Test
    public void rejectNegativeLength() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setLength(-1));
    }

    @Test
    public void rejectNegativeWidth() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setWidth(-1));
    }

    @Test
    public void rejectNegativeHeight() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setHeight(-1));
    }

    @Test
    public void rejectEmptyDescription() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().addDescription(""));
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().addDescription(null));
    }

    @Test
    public void rejectEmptyNameElement() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setName(""));
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setName(null));
    }

    @Test
    public void rejectEmptyMediaTypeElement() {
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setMediaType(""));
        assertThrows(IllegalArgumentException.class, () -> FileMetadataElement.builder().setMediaType(null));
    }

    @Test
    public void getDescTest() {
        FileMetadataElement metadataElement = FileMetadataElement.builder()
                .addDescription("Foo", "br")
                .addDescription("Baz")
                .addDescription("Bag", "en")
                .build();

        assertEquals("Foo", metadataElement.getDescription("br"));
        assertEquals("Baz", metadataElement.getDescription(null));
        assertEquals("Baz", metadataElement.getDescription());
        assertEquals("Bag", metadataElement.getDescription("en"));
        assertNull(metadataElement.getDescription("null"));
        assertEquals(3, metadataElement.getDescriptions().size());
    }
}
