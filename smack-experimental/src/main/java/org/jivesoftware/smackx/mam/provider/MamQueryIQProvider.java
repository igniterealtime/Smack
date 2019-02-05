/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.mam.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * MAM Query IQ Provider class.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez
 *
 */
public class MamQueryIQProvider extends IQProvider<MamQueryIQ> {

    @Override
    public MamQueryIQ parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException, ParseException {
        DataForm dataForm = null;
        String queryId = parser.getAttributeValue("", "queryid");
        String node = parser.getAttributeValue("", "node");

        outerloop: while (true) {
            final int eventType = parser.next();
            final String name = parser.getName();

            switch (eventType) {
            case XmlPullParser.START_TAG:
                switch (name) {
                case DataForm.ELEMENT:
                    dataForm = DataFormProvider.INSTANCE.parse(parser);
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

        return new MamQueryIQ(queryId, node, dataForm);
    }

}
