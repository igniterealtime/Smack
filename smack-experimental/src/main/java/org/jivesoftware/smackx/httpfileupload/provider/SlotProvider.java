/**
 *
 * Copyright © 2017 Grigory Fedorov
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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadService;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.Slot_V0_2;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Provider for Slot.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class SlotProvider extends IQProvider<Slot> {

    @Override
    public Slot parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        final String namespace = parser.getNamespace();

        final UploadService.Version version = HttpFileUploadManager.namespaceToVersion(namespace);
        assert version != null;

        URL putUrl = null;
        URL getUrl = null;
        Map<String, String> headers = null;

        outerloop: while (true) {
            int event = parser.next();

            switch (event) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case "put": {
                            String putUrlString;
                            switch (version) {
                            case v0_2:
                                putUrlString = parser.nextText();
                                break;
                            case v0_3:
                                putUrlString = parser.getAttributeValue(null, "url");
                                break;
                            default:
                                throw new AssertionError();
                            }
                            putUrl = new URL(putUrlString);
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
                        case "header":
                            String headerName = ParserUtils.getRequiredAttribute(parser, "name");
                            String headerValue = ParserUtils.getRequiredNextText(parser);
                            if (headers == null) {
                                headers = new HashMap<>();
                            }
                            headers.put(headerName, headerValue);
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        switch (version) {
        case v0_3:
            return new Slot(putUrl, getUrl, headers);
        case v0_2:
            return new Slot_V0_2(putUrl, getUrl);
        default:
            throw new AssertionError();
        }
    }

}
