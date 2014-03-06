/**
 *
 * Copyright 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.xhtmlim.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class XHTMLExtensionProviderTest {
    public static final String XHTML_EXTENSION_SAMPLE_RESOURCE_NAME = "xhtml.xml";

    @Test
    public void parsesWell() throws IOException, XmlPullParserException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(getClass().getResourceAsStream(XHTML_EXTENSION_SAMPLE_RESOURCE_NAME), "UTF-8");
        parser.next();

        XHTMLExtensionProvider provider = new XHTMLExtensionProvider();
        PacketExtension extension = provider.parseExtension(parser);

        assertThat(extension, is(instanceOf(XHTMLExtension.class)));
        XHTMLExtension attachmentsInfo = (XHTMLExtension) extension;

        assertEquals(sampleXhtml(), attachmentsInfo.getBodies().next());
    }

    private String sampleXhtml() {
        return "<body xmlns='http://www.w3.org/1999/xhtml'>" +
                "<span style='color: rgb(0, 0, 0); font-family: sans-serif, &apos;trebuchet ms&apos;" +
                ", &apos;lucida grande&apos;, &apos;lucida sans unicode&apos;, arial, helvetica, " +
                "sans-serif; font-weight: 600; line-height: 18px;'>Generic family<br/>AnotherLine</span></body>";
    }
}
