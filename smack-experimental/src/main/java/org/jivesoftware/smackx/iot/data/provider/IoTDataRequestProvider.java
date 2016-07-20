/**
 *
 * Copyright © 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.iot.data.element.IoTDataRequest;
import org.xmlpull.v1.XmlPullParser;

public class IoTDataRequestProvider extends IQProvider<IoTDataRequest> {

    @Override
    public IoTDataRequest parse(XmlPullParser parser, int initialDepth) throws Exception {
        int seqNr = ParserUtils.getIntegerAttributeOrThrow(parser, "seqnr", "IoT data request without sequence number");
        boolean momentary = ParserUtils.getBooleanAttribute(parser, "momentary", false);
        return new IoTDataRequest(seqNr, momentary);
    }

}
