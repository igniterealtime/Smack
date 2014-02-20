/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.FileUtils;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;

public class ExtensionsStartupClasses implements SmackInitializer {

    private static final String EXTENSIONS_XML = "classpath:org.jivesoftware.smackx/extensions.xml";

    private List<Exception> exceptions = new LinkedList<Exception>();
    // TODO log

    @Override
    public void initialize() {
        InputStream is;
        XmlPullParser parser;
        int eventType;
        try {
            is = FileUtils.getStreamForUrl(EXTENSIONS_XML, null);
            parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(is, "UTF-8");
            eventType = parser.getEventType();
        }
        catch (Exception e) {
            exceptions.add(e);
            return;
        }
        try {
            do {
                String name = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if ("startupClasses".equals(name)) {
                        try {
                            SmackConfiguration.parseClassesToLoad(parser, false);
                        }
                        catch (Exception e) {
                            exceptions.add(e);
                        }
                    }
                }
                eventType = parser.next();
            }
            while (eventType != XmlPullParser.END_DOCUMENT);
            is.close();
        }
        catch (Exception e) {
            exceptions.add(e);
        }
    }

    @Override
    public List<Exception> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }

}
