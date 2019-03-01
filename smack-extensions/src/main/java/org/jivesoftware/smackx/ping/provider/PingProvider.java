/**
 *
 * Copyright 2012 Florian Schmaus
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
package org.jivesoftware.smackx.ping.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.ping.packet.Ping;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PingProvider extends IQProvider<Ping> {

    @Override
    public Ping parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        // No need to use the ping constructor with arguments. IQ will already
        // have filled out all relevant fields ('from', 'to', 'id').
        return new Ping();
    }

}
