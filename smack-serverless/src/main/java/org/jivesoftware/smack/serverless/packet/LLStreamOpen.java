/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.serverless.packet;

import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class LLStreamOpen extends StreamOpen {

    private final String fromService;

    public LLStreamOpen(String toService, String fromService) {
        super(toService);
        this.fromService = fromService;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = getBasicStreamOpen();
        xml.attribute("from", fromService);
        xml.rightAngleBracket();
        return xml;
    }

}
