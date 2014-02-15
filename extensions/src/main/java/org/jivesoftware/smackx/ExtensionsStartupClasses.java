package org.jivesoftware.smackx;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.FileUtils;
import org.xmlpull.mxp1.MXParser;
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
            parser = new MXParser();
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
