/**
 *
 * Copyright © 2014-2021 Florian Schmaus
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
package org.jivesoftware.smackx.time.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.time.packet.Time;
import org.jivesoftware.smackx.time.packet.TimeBuilder;

public class TimeProvider extends IqProvider<Time> {

    @Override
    public Time parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, ParseException {
        String utc = null, tzo = null;
        TimeBuilder timeBuilder = Time.builder(iqData);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "utc":
                    utc = parser.nextText();
                    break;
                case "tzo":
                    tzo = parser.nextText();
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                break;
            }
        }

        if (utc != null) {
            timeBuilder.setUtcAndTzo(utc, tzo);
        }

        return timeBuilder.build();
    }

}
