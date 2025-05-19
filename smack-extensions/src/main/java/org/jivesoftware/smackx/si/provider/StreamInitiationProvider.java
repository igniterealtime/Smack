/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.si.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jivesoftware.smackx.thumbnail.element.Thumbnail;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jxmpp.util.XmppDateTime;

/**
 * The StreamInitiationProvider parses StreamInitiation packets with thumbnail element support.
 *
 * @author Alexander Wenckus
 * @author Eng Chong Meng
 */
public class StreamInitiationProvider extends IQProvider<StreamInitiation> {
    private static final Logger LOGGER = Logger.getLogger(StreamInitiationProvider.class.getName());

    /**
     * Parses the given <code>parser</code> in order to create a <code>FileElement</code> from it.
     *
     * @param parser the parser to parse
     */
    @Override
    public StreamInitiation parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws IOException, XmlPullParserException, SmackParsingException {
        boolean done = false;

        // si
        String id = parser.getAttributeValue("", "id");
        String mimeType = parser.getAttributeValue("", "mime-type");

        StreamInitiation initiation = new StreamInitiation();

        // file
        String name = null;
        String size = null;
        String hash = null;
        String date = null;
        String desc = null;
        Thumbnail thumbnail = null;
        boolean isRanged = false;

        // feature
        DataForm form = null;
        DataFormProvider dataFormProvider = new DataFormProvider();

        XmlPullParser.Event eventType;
        String elementName;
        String namespace;
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (elementName.equals("file")) {
                    name = parser.getAttributeValue("", StreamInitiation.ATTR_NAME);
                    size = parser.getAttributeValue("", StreamInitiation.ATTR_SIZE);
                    hash = parser.getAttributeValue("", StreamInitiation.ATTR_HASH);
                    date = parser.getAttributeValue("", StreamInitiation.ATTR_DATE);
                }
                else if (elementName.equals(StreamInitiation.ELEM_DESC)) {
                    desc = parser.nextText();
                }
                else if (elementName.equals(StreamInitiation.ELEM_RANGE)) {
                    isRanged = true;
                }
                else if (elementName.equals("x") && namespace.equals("jabber:x:data")) {
                    form = dataFormProvider.parse(parser);
                }
                else if (elementName.equals("thumbnail")) {
                    thumbnail = new Thumbnail(parser);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (elementName.equals("si")) {
                    done = true;
                }
                // The name-attribute is required per XEP-0096, so ignore the IQ if the name is not
                // set to avoid exceptions. Particularly,
                // the SI response of Empathy contains an invalid, empty file-tag.
                else if (elementName.equals("file") && (name != null)) {
                    long fileSize = 0;
                    size = StringUtils.returnIfNotEmptyTrimmed(size);
                    if (size != null) {
                        try {
                            fileSize = Long.parseLong(size);
                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.SEVERE, "Failed to parse file size from " + fileSize);
                        }
                    }

                    StreamInitiation.File file = new StreamInitiation.File(name, fileSize);
                    if (date != null) {
                        try {
                            file.setDate(XmppDateTime.parseDate(date));
                        } catch (ParseException e) {
                            LOGGER.log(Level.WARNING, "Unknown date format on incoming file transfer: " + date);
                        }
                    }
                    else {
                        file.setDate(new Date());
                    }
                    file.setDesc(desc);
                    file.setRanged(isRanged);
                    file.setHash(hash);
                    file.setThumbnail(thumbnail);
                    initiation.setFile(file);
                }
            }
        }
        initiation.setSessionID(id);
        initiation.setMimeType(mimeType);
        initiation.setFeatureNegotiationForm(form);
        return initiation;
    }
}
