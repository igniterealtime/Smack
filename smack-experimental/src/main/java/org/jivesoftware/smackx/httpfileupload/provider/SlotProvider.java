/**
 *
 * Copyright © 2017 Grigory Fedorov, 2017-2019 Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload.provider;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadService;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.Slot_V0_2;

/**
 * Provider for Slot.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class SlotProvider extends IqProvider<Slot> {

    @Override
    public Slot parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        final String namespace = parser.getNamespace();

        final UploadService.Version version = HttpFileUploadManager.namespaceToVersion(namespace);
        assert version != null;

        URL putUrl = null;
        URL getUrl = null;
        PutElement_V0_4_Content putElementV04Content = null;

        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();

            switch (event) {
                case START_ELEMENT:
                    String name = parser.getName();
                    switch (name) {
                        case "put": {
                            switch (version) {
                            case v0_2:
                                String putUrlString = parser.nextText();
                                putUrl = new URL(putUrlString);
                                break;
                            case v0_3:
                                putElementV04Content = parsePutElement_V0_4(parser);
                                break;
                            default:
                                throw new AssertionError();
                            }
                            break;
                        }
                        case "get":
                            String getUrlString;
                            switch (version) {
                            case v0_2:
                                getUrlString = parser.nextText();
                                break;
                            case v0_3:
                                getUrlString = parser.getAttributeValue(null, "url");
                                break;
                            default:
                                throw new AssertionError();
                            }
                            getUrl = new URL(getUrlString);
                            break;
                    }
                    break;
                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }

        switch (version) {
        case v0_3:
            return new Slot(putElementV04Content.putUrl, getUrl, putElementV04Content.headers);
        case v0_2:
            return new Slot_V0_2(putUrl, getUrl);
        default:
            throw new AssertionError();
        }
    }

    public static PutElement_V0_4_Content parsePutElement_V0_4(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();

        String putUrlString = parser.getAttributeValue(null, "url");
        URL putUrl = new URL(putUrlString);

        Map<String, String> headers = null;
        outerloop: while (true) {
            XmlPullParser.Event next = parser.next();
            switch (next) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "header":
                    String headerName = ParserUtils.getRequiredAttribute(parser, "name");
                    String headerValue = ParserUtils.getRequiredNextText(parser);
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    headers.put(headerName, headerValue);
                    break;
                default:
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return new PutElement_V0_4_Content(putUrl, headers);
    }

    public static final class PutElement_V0_4_Content {
        private final URL putUrl;
        private final Map<String, String> headers;

        private PutElement_V0_4_Content(URL putUrl, Map<String, String> headers) {
            this.putUrl = putUrl;
            this.headers = headers;
        }

        public URL getPutUrl() {
            return putUrl;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
