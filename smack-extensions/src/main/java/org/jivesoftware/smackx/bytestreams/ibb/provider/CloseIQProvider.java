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
package org.jivesoftware.smackx.bytestreams.ibb.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.bytestreams.ibb.packet.Close;

import org.xmlpull.v1.XmlPullParser;

/**
 * Parses a close In-Band Bytestream packet.
 *
 * @author Henning Staib
 */
public class CloseIQProvider extends IQProvider<Close> {

    @Override
    public Close parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        String sid = parser.getAttributeValue("", "sid");
        return new Close(sid);
    }

}
