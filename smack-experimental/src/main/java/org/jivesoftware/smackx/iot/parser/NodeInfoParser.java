/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.parser;

import static org.jivesoftware.smack.util.StringUtils.isNullOrEmpty;

import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.iot.element.NodeInfo;

public class NodeInfoParser {

    public static NodeInfo parse(XmlPullParser parser) {
        String nodeId = parser.getAttributeValue(null, "nodeId");
        String sourceId = parser.getAttributeValue(null, "sourceId");
        String cacheType = parser.getAttributeValue(null, "cacheType");
        if (isNullOrEmpty(nodeId) && isNullOrEmpty(sourceId) && isNullOrEmpty(cacheType)) {
            return NodeInfo.EMPTY;
        }
        return new NodeInfo(nodeId, sourceId, cacheType);
    }
}
