/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.ox.provider;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class OpenPgpContentElementProvider<O extends OpenPgpContentElement> extends ExtensionElementProvider<O> {

    public static OpenPgpContentElement parseOpenPgpContentElement(String element)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = PacketParserUtils.getParserFor(element);
        return parseOpenPgpContentElement(parser);
    }

    public static OpenPgpContentElement parseOpenPgpContentElement(XmlPullParser parser) {
        return null;
    }

    @Override
    public abstract O parse(XmlPullParser parser, int initialDepth) throws Exception;

    protected static OpenPgpContentElementData parseOpenPgpContentElementData(XmlPullParser parser, int initialDepth) {
        List<Jid> to = new LinkedList<>();
        Date timestamp = null;
        String rpad = null;
        List<ExtensionElement> payload = new LinkedList<>();

        return new OpenPgpContentElementData(to, timestamp, rpad, payload);
    }

    protected final static class OpenPgpContentElementData {
        protected final List<Jid> to;
        protected final Date timestamp;
        protected final String rpad;
        protected final List<ExtensionElement> payload;

        private OpenPgpContentElementData(List<Jid> to, Date timestamp, String rpad, List<ExtensionElement> payload) {
            this.to = to;
            this.timestamp = timestamp;
            this.rpad = rpad;
            this.payload = payload;
        }
    }
}
