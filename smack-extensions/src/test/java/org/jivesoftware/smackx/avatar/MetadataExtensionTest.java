/*
 *
 * Copyright 2017 Fernando Ramirez, 2021 Paul Schaub
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
package org.jivesoftware.smackx.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.test.util.SmackTestUtil;

import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.jivesoftware.smackx.avatar.provider.MetadataProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class MetadataExtensionTest {

    private static final String metadataExtensionExample = "<metadata xmlns='urn:xmpp:avatar:metadata'>"
            + "<info "
            + "id='357a8123a30844a3aa99861b6349264ba67a5694' "
            + "bytes='23456' "
            + "type='image/gif' "
            + "url='http://avatars.example.org/happy.gif' "
            + "height='64' "
            + "width='128'/>"
            + "</metadata>";

    private static final String emptyMetadataExtensionExample = "<metadata xmlns='urn:xmpp:avatar:metadata'/>";

    private static final String metadataWithSeveralInfos = "<metadata xmlns='urn:xmpp:avatar:metadata'>"
            + "<info bytes='12345'"
            + " height='64'"
            + " id='111f4b3c50d7b0df729d299bc6f8e9ef9066971f'"
            + " type='image/png'"
            + " width='64'/>"
            + "<info bytes='12345'"
            + " height='64'"
            + " id='e279f80c38f99c1e7e53e262b440993b2f7eea57'"
            + " type='image/png'"
            + " url='http://avatars.example.org/happy.png'"
            + " width='128'/>"
            + "<info bytes='23456'"
            + " height='64'"
            + " id='357a8123a30844a3aa99861b6349264ba67a5694'"
            + " type='image/gif'"
            + " url='http://avatars.example.org/happy.gif'"
            + " width='64'/>"
            + "</metadata>";

    private static final String metadataWithInfoAndPointers = "<metadata xmlns='urn:xmpp:avatar:metadata'>"
            + "<info"
            + " id='111f4b3c50d7b0df729d299bc6f8e9ef9066971f'"
            + " bytes='12345'"
            + " type='image/png'"
            + " height='64'"
            + " width='64'/>"
            + "<pointer>"
            + "<x xmlns='http://example.com/virtualworlds'>"
            + "<game>Ancapistan</game>"
            + "<character>Kropotkin</character>"
            + "</x>"
            + "</pointer>"
            + "<pointer>"
            + "<x xmlns='http://sample.com/game'>"
            + "<level>hard</level>"
            + "<players>2</players>"
            + "</x>"
            + "</pointer>"
            + "</metadata>";

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void checkMetadataExtensionParse(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String id = "357a8123a30844a3aa99861b6349264ba67a5694";
        URL url = new URL("http://avatars.example.org/happy.gif");
        long bytes = 23456;
        String type = "image/gif";
        int pixelsHeight = 64;
        int pixelsWidth = 128;

        MetadataInfo info = new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);
        List<MetadataInfo> infos = new ArrayList<>();
        infos.add(info);

        MetadataExtension metadataExtension = new MetadataExtension(infos);
        assertEquals(metadataExtensionExample, metadataExtension.toXML().toString());

        MetadataExtension metadataExtensionFromProvider = SmackTestUtil
                .parse(metadataExtensionExample, MetadataProvider.class, parserKind);

        assertEquals(id, metadataExtensionFromProvider.getInfoElements().get(0).getId());
        assertEquals(url, metadataExtensionFromProvider.getInfoElements().get(0).getUrl());
        assertEquals(bytes, metadataExtensionFromProvider.getInfoElements().get(0).getBytes().intValue());
        assertEquals(type, metadataExtensionFromProvider.getInfoElements().get(0).getType());
        assertEquals(pixelsHeight, metadataExtensionFromProvider.getInfoElements().get(0).getHeight().intValue());
        assertEquals(pixelsWidth, metadataExtensionFromProvider.getInfoElements().get(0).getWidth().intValue());
    }

    @Test
    public void checkEmptyMetadataExtensionParse() {
        MetadataExtension metadataExtension = new MetadataExtension();
        assertEquals(emptyMetadataExtensionExample, metadataExtension.toXML().toString());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void checkSeveralInfosInMetadataExtension(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        MetadataExtension metadataExtensionFromProvider = SmackTestUtil
                .parse(metadataWithSeveralInfos, MetadataProvider.class, parserKind);

        MetadataInfo info1 = metadataExtensionFromProvider.getInfoElements().get(0);
        MetadataInfo info2 = metadataExtensionFromProvider.getInfoElements().get(1);
        MetadataInfo info3 = metadataExtensionFromProvider.getInfoElements().get(2);

        assertEquals("111f4b3c50d7b0df729d299bc6f8e9ef9066971f", info1.getId());
        assertNull(info1.getUrl());
        assertEquals(12345, info1.getBytes().intValue());
        assertEquals("image/png", info1.getType());
        assertEquals(64, info1.getHeight().intValue());
        assertEquals(64, info1.getWidth().intValue());

        assertEquals("e279f80c38f99c1e7e53e262b440993b2f7eea57", info2.getId());
        assertEquals(new URL("http://avatars.example.org/happy.png"), info2.getUrl());
        assertEquals(12345, info2.getBytes().intValue());
        assertEquals("image/png", info2.getType());
        assertEquals(64, info2.getHeight().intValue());
        assertEquals(128, info2.getWidth().intValue());

        assertEquals("357a8123a30844a3aa99861b6349264ba67a5694", info3.getId());
        assertEquals(new URL("http://avatars.example.org/happy.gif"), info3.getUrl());
        assertEquals(23456, info3.getBytes().intValue());
        assertEquals("image/gif", info3.getType());
        assertEquals(64, info3.getHeight().intValue());
        assertEquals(64, info3.getWidth().intValue());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void checkInfosAndPointersParse(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        MetadataExtension metadataExtensionFromProvider = SmackTestUtil
                .parse(metadataWithInfoAndPointers, MetadataProvider.class, parserKind);

        MetadataInfo info = metadataExtensionFromProvider.getInfoElements().get(0);
        assertEquals("111f4b3c50d7b0df729d299bc6f8e9ef9066971f", info.getId());
        assertNull(info.getUrl());
        assertEquals(12345, info.getBytes().intValue());
        assertEquals("image/png", info.getType());
        assertEquals(64, info.getHeight().intValue());
        assertEquals(64, info.getWidth().intValue());

        MetadataPointer pointer1 = metadataExtensionFromProvider.getPointerElements().get(0);
        Map<String, Object> fields1 = pointer1.getFields();
        assertEquals("http://example.com/virtualworlds", pointer1.getNamespace());
        assertEquals("Ancapistan", fields1.get("game"));
        assertEquals("Kropotkin", fields1.get("character"));

        MetadataPointer pointer2 = metadataExtensionFromProvider.getPointerElements().get(1);
        Map<String, Object> fields2 = pointer2.getFields();
        assertEquals("http://sample.com/game", pointer2.getNamespace());
        assertEquals("hard", fields2.get("level"));
        assertEquals("2", fields2.get("players"));
    }

    @Test
    public void createMetadataExtensionWithInfoAndPointer() {
        String id = "111f4b3c50d7b0df729d299bc6f8e9ef9066971f";
        long bytes = 12345;
        String type = "image/png";
        int pixelsHeight = 64;
        int pixelsWidth = 64;
        MetadataInfo info = new MetadataInfo(id, null, bytes, type, pixelsHeight, pixelsWidth);

        HashMap<String, Object> fields1 = new HashMap<>();
        fields1.put("game", "Ancapistan");
        fields1.put("character", "Kropotkin");
        MetadataPointer pointer1 = new MetadataPointer("http://example.com/virtualworlds", fields1);

        HashMap<String, Object> fields2 = new HashMap<>();
        fields2.put("level", "hard");
        fields2.put("players", 2);
        MetadataPointer pointer2 = new MetadataPointer("http://sample.com/game", fields2);

        List<MetadataInfo> infos = new ArrayList<>();
        infos.add(info);

        List<MetadataPointer> pointers = new ArrayList<>();
        pointers.add(pointer1);
        pointers.add(pointer2);

        MetadataExtension metadataExtension = new MetadataExtension(infos, pointers);
        assertEquals(metadataWithInfoAndPointers, metadataExtension.toXML().toString());
    }

}
