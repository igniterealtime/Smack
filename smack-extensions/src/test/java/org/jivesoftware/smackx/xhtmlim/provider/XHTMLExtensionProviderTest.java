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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jivesoftware.smack.test.util.CharSequenceEquals.equalsCharSequence;

import java.io.IOException;
import java.io.InputStream;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;

import org.junit.Test;

public class XHTMLExtensionProviderTest {
    public static final String XHTML_EXTENSION_SAMPLE_RESOURCE_NAME = "xhtml.xml";

    @Test
    public void parsesWell() throws IOException, XmlPullParserException {
        InputStream inputStream = getClass().getResourceAsStream(XHTML_EXTENSION_SAMPLE_RESOURCE_NAME);
        XmlPullParser parser = PacketParserUtils.getParserFor(inputStream);
        parser.next();

        XHTMLExtensionProvider provider = new XHTMLExtensionProvider();
        ExtensionElement extension = provider.parse(parser, parser.getDepth(), null);

        assertThat(extension, instanceOf(XHTMLExtension.class));
        XHTMLExtension attachmentsInfo = (XHTMLExtension) extension;

        assertThat(sampleXhtml(), equalsCharSequence(attachmentsInfo.getBodies().get(0)));
    }

    private static String sampleXhtml() {
        return "<body xmlns='http://www.w3.org/1999/xhtml'>" +
                "<span style='color: rgb(0, 0, 0); font-family: sans-serif, &apos;trebuchet ms&apos;" +
                ", &apos;lucida grande&apos;, &apos;lucida sans unicode&apos;, arial, helvetica, " +
                "sans-serif; font-weight: 600; line-height: 18px;'>Generic family<br/>AnotherLine</span></body>";
    }
}
