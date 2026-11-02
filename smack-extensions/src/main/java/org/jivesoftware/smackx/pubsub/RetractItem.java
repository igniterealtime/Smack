/*
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a request to retract a node item.
 *
 * @author Robin Collier
 * @author Eng Chong Meng
 */
public class RetractItem extends NodeExtension {

    public static String ELE_ITEM = "item";
    public static String ATTR_ID = "id";
    protected final String mId;

    public RetractItem(String nodeId, String id) {
        super(PubSubElementType.RETRACT, nodeId);
        mId = id;
    }

    public String getId() {
        return mId;
    }

    @Override
    protected void addXml(XmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.halfOpenElement(ELE_ITEM);
        xml.attribute(ATTR_ID, getId());
        xml.closeEmptyElement();
        xml.closeElement(getElementName());
    }
}
