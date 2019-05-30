/**
 *
 * Copyright 2019 Aditya Borikar
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
package org.jivesoftware.smackx.dataformmedia;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MediaElementTest {

    @Test
    public void toXmlTestForAudio() throws URISyntaxException {
        String xml = "<media xmlns='urn:xmpp:media-element'>" +
                "<uri type='audio/x-wav'>" +
                "http://victim.example.com/challenges/speech.wav?F3A6292C" +
                "</uri>" +
                "<uri type='audio/ogg; codecs=speex'>" +
                "cid:sha1+a15a505e360702b79c75a5f67773072ed392f52a@bob.xmpp.org" +
                "</uri>" +
                "<uri type='audio/mpeg'>" +
                "http://victim.example.com/challenges/speech.mp3?F3A6292C" +
                "</uri>" +
                "</media>";
        URI uri1 = new URI("http://victim.example.com/challenges/speech.wav?F3A6292C");
        URI uri2 = new URI("cid:sha1+a15a505e360702b79c75a5f67773072ed392f52a@bob.xmpp.org");
        URI uri3 = new URI("http://victim.example.com/challenges/speech.mp3?F3A6292C");

        List<URINode> uriNodeList = new ArrayList<URINode>();

        URINode urinode1 = new URINode("audio/x-wav", uri1);
        URINode urinode2 = new URINode("audio/ogg", "codecs=speex", uri2);
        URINode urinode3 = new URINode("audio/mpeg", uri3);

        uriNodeList.add(urinode1);
        uriNodeList.add(urinode2);
        uriNodeList.add(urinode3);

        MediaElement mediaElement = new MediaElement(uriNodeList);
        assertXmlSimilar(xml, mediaElement.toXML().toString());
    }

    @Test
    public void toXmlTestForImages() throws URISyntaxException {
           String xml = "<media xmlns='urn:xmpp:media-element'" +
                   " height='80'" +
                   " width='290'>" +
                   "<uri type='image/jpeg'>" +
                   "http://www.victim.com/challenges/ocr.jpeg?F3A6292C" +
                   "</uri>" +
                   "<uri type='image/jpeg'>" +
                   "cid:sha1+f24030b8d91d233bac14777be5ab531ca3b9f102@bob.xmpp.org" +
                   "</uri>" +
                   "</media>";
           URI uri1 = new URI("http://www.victim.com/challenges/ocr.jpeg?F3A6292C");
           URI uri2 = new URI("cid:sha1+f24030b8d91d233bac14777be5ab531ca3b9f102@bob.xmpp.org");

           URINode urinode1 = new URINode("image/jpeg", uri1);
           URINode urinode2 = new URINode("image/jpeg", uri2);

           List<URINode> uriNodeList = new ArrayList<URINode>();
           uriNodeList.add(urinode1);
           uriNodeList.add(urinode2);

           MediaElement mediaElement = new MediaElement(uriNodeList, 80, 290);
           assertXmlSimilar(xml, mediaElement.toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void atLeastOneUriTest() {
        List<URINode> uriNodeList = new ArrayList<URINode>();
        new MediaElement(uriNodeList);
    }
}
