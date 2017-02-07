/**
 *
 * Copyright 2017 Fernando Ramirez
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.jivesoftware.smackx.avatar.provider.MetadataProvider;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class MetadataExtensionTest {

    // @formatter:off
    String metadataExtensionExample = "<metadata xmlns='urn:xmpp:avatar:metadata'>" 
            + "<info "
            + "id='357a8123a30844a3aa99861b6349264ba67a5694' " 
            + "bytes='23456' " 
            + "type='image/gif' "
            + "url='http://avatars.example.org/happy.gif' " 
            + "height='64' " 
            + "width='128'/>" 
            + "</metadata>";
    // @formatter:on

    // @formatter:off
    String emptyMetadataExtensionExample = "<metadata xmlns='urn:xmpp:avatar:metadata'/>";
    // @formatter:on

    // @formatter:off
    String metadataWithSeveralInfos = "<metadata xmlns='urn:xmpp:avatar:metadata'>"
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
    // @formatter:on

    // @formatter:off
    String metadataWithInfoAndPointers = "<metadata xmlns='urn:xmpp:avatar:metadata'>"
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
    // @formatter:on

    @Test
    public void checkMetadataExtensionParse() throws Exception {
        String id = "357a8123a30844a3aa99861b6349264ba67a5694";
        String url = "http://avatars.example.org/happy.gif";
        long bytes = 23456;
        String type = "image/gif";
        int pixelsHeight = 64;
        int pixelsWidth = 128;

        MetadataInfo info = new MetadataInfo(id, url, bytes, type, pixelsHeight, pixelsWidth);
        List<MetadataInfo> infos = new ArrayList<>();
        infos.add(info);

        MetadataExtension metadataExtension = new MetadataExtension(infos);
        Assert.assertEquals(metadataExtensionExample, metadataExtension.toXML().toString());

        XmlPullParser parser = PacketParserUtils.getParserFor(metadataExtensionExample);
        MetadataExtension metadataExtensionFromProvider = new MetadataProvider().parse(parser);

        Assert.assertEquals(id, metadataExtensionFromProvider.getInfos().get(0).getId());
        Assert.assertEquals(url, metadataExtensionFromProvider.getInfos().get(0).getUrl());
        Assert.assertEquals(bytes, metadataExtensionFromProvider.getInfos().get(0).getBytes());
        Assert.assertEquals(type, metadataExtensionFromProvider.getInfos().get(0).getType());
        Assert.assertEquals(pixelsHeight, metadataExtensionFromProvider.getInfos().get(0).getHeight());
        Assert.assertEquals(pixelsWidth, metadataExtensionFromProvider.getInfos().get(0).getWidth());
    }

    @Test
    public void checkEmptyMetadataExtensionParse() throws Exception {
        MetadataExtension metadataExtension = new MetadataExtension(null);
        Assert.assertEquals(emptyMetadataExtensionExample, metadataExtension.toXML().toString());
    }

    @Test
    public void checkSeveralInfosInMetadataExtension() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(metadataWithSeveralInfos);
        MetadataExtension metadataExtensionFromProvider = new MetadataProvider().parse(parser);

        MetadataInfo info1 = metadataExtensionFromProvider.getInfos().get(0);
        MetadataInfo info2 = metadataExtensionFromProvider.getInfos().get(1);
        MetadataInfo info3 = metadataExtensionFromProvider.getInfos().get(2);

        Assert.assertEquals("111f4b3c50d7b0df729d299bc6f8e9ef9066971f", info1.getId());
        Assert.assertNull(info1.getUrl());
        Assert.assertEquals(12345, info1.getBytes());
        Assert.assertEquals("image/png", info1.getType());
        Assert.assertEquals(64, info1.getHeight());
        Assert.assertEquals(64, info1.getWidth());

        Assert.assertEquals("e279f80c38f99c1e7e53e262b440993b2f7eea57", info2.getId());
        Assert.assertEquals("http://avatars.example.org/happy.png", info2.getUrl());
        Assert.assertEquals(12345, info2.getBytes());
        Assert.assertEquals("image/png", info2.getType());
        Assert.assertEquals(64, info2.getHeight());
        Assert.assertEquals(128, info2.getWidth());

        Assert.assertEquals("357a8123a30844a3aa99861b6349264ba67a5694", info3.getId());
        Assert.assertEquals("http://avatars.example.org/happy.gif", info3.getUrl());
        Assert.assertEquals(23456, info3.getBytes());
        Assert.assertEquals("image/gif", info3.getType());
        Assert.assertEquals(64, info3.getHeight());
        Assert.assertEquals(64, info3.getWidth());
    }

    @Test
    public void checkInfosAndPointersParse() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(metadataWithInfoAndPointers);
        MetadataExtension metadataExtensionFromProvider = new MetadataProvider().parse(parser);

        MetadataInfo info = metadataExtensionFromProvider.getInfos().get(0);
        Assert.assertEquals("111f4b3c50d7b0df729d299bc6f8e9ef9066971f", info.getId());
        Assert.assertNull(info.getUrl());
        Assert.assertEquals(12345, info.getBytes());
        Assert.assertEquals("image/png", info.getType());
        Assert.assertEquals(64, info.getHeight());
        Assert.assertEquals(64, info.getWidth());

        MetadataPointer pointer1 = metadataExtensionFromProvider.getPointers().get(0);
        HashMap<String, Object> fields1 = pointer1.getFields();
        Assert.assertEquals("http://example.com/virtualworlds", pointer1.getNamespace());
        Assert.assertEquals("Ancapistan", fields1.get("game"));
        Assert.assertEquals("Kropotkin", fields1.get("character"));

        MetadataPointer pointer2 = metadataExtensionFromProvider.getPointers().get(1);
        HashMap<String, Object> fields2 = pointer2.getFields();
        Assert.assertEquals("http://sample.com/game", pointer2.getNamespace());
        Assert.assertEquals("hard", fields2.get("level"));
        Assert.assertEquals("2", fields2.get("players"));
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
        Assert.assertEquals(metadataWithInfoAndPointers, metadataExtension.toXML().toString());
    }

}
