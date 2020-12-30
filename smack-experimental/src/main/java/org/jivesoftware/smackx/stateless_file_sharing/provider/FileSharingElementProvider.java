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
package org.jivesoftware.smackx.stateless_file_sharing.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;
import org.jivesoftware.smackx.file_metadata.provider.FileMetadataElementProvider;
import org.jivesoftware.smackx.stateless_file_sharing.element.FileSharingElement;
import org.jivesoftware.smackx.stateless_file_sharing.element.SourcesElement;
import org.jivesoftware.smackx.url_address_information.element.UrlDataElement;
import org.jivesoftware.smackx.url_address_information.provider.UrlDataElementProvider;

public class FileSharingElementProvider extends ExtensionElementProvider<FileSharingElement> {

    public static final FileSharingElementProvider INSTANCE = new FileSharingElementProvider();

    @Override
    public FileSharingElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        FileMetadataElement fileMetadataElement = null;
        SourcesElement sourcesElement = null;
        List<UrlDataElement> urlDataElements = new ArrayList<>();
        List<ExtensionElement> otherSourceElements = new ArrayList<>();
        do {
            XmlPullParser.TagEvent event = parser.nextTag();
            String name = parser.getName();

            if (event == XmlPullParser.TagEvent.START_ELEMENT) {
                if (name.equals(FileMetadataElement.ELEMENT)) {
                    fileMetadataElement = FileMetadataElementProvider.TEST_INSTANCE.parse(parser, xmlEnvironment);
                } else if (name.equals(SourcesElement.ELEMENT)) {
                    int innerDepth = parser.getDepth();
                    do {
                        XmlPullParser.TagEvent innerEvent = parser.nextTag();
                        String innerName = parser.getName();
                        if (innerEvent.equals(XmlPullParser.TagEvent.START_ELEMENT)) {
                            if (innerName.equals(UrlDataElement.ELEMENT)) {
                                urlDataElements.add(UrlDataElementProvider.INSTANCE.parse(parser));
                            } else {
                                ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(innerName, parser.getNamespace());
                                if (provider == null) {
                                    provider = new StandardExtensionElementProvider();
                                }
                                otherSourceElements.add(provider.parse(parser));
                            }
                        } else {
                            if (innerName.equals(SourcesElement.ELEMENT)) {
                                sourcesElement = new SourcesElement(urlDataElements, otherSourceElements);
                            }
                        }
                    } while (parser.getDepth() != innerDepth);
                }
            }
        } while (parser.getDepth() != initialDepth);
        return new FileSharingElement(fileMetadataElement, sourcesElement);
    }
}
