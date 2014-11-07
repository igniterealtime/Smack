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
package org.jivesoftware.smackx.iqprivate.packet;

import org.jivesoftware.smack.packet.IQ;

public class PrivateDataIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:private";

    private final PrivateData privateData;
    private final String getElement;
    private final String getNamespace;

    public PrivateDataIQ(PrivateData privateData) {
        this(privateData, null, null);
        setType(Type.set);
    }

    public PrivateDataIQ(String element, String namespace) {
        this(null, element, namespace);
        setType(Type.get);
    }

    private PrivateDataIQ(PrivateData privateData, String getElement, String getNamespace) {
        super(ELEMENT, NAMESPACE);
        this.privateData = privateData;
        this.getElement = getElement;
        this.getNamespace = getNamespace;
    }

    public PrivateData getPrivateData() {
        return privateData;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        if (privateData != null) {
            xml.append(privateData.toXML());
        } else {
            xml.halfOpenElement(getElement).xmlnsAttribute(getNamespace).closeEmptyElement();
        }
        return xml;
    }
}
