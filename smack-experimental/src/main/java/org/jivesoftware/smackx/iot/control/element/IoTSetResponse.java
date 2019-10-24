/**
 *
 * Copyright © 2016-2019 Florian Schmaus
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
package org.jivesoftware.smackx.iot.control.element;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;

public class IoTSetResponse extends IQ {

    public static final String ELEMENT = "setResponse";
    public static final String NAMESPACE = Constants.IOT_CONTROL_NAMESPACE;

    public IoTSetResponse(IqBuilder iqBuilder) {
        super(iqBuilder, ELEMENT, NAMESPACE);
    }

    // TODO: Deprecate when stanza build is ready.
    public IoTSetResponse() {
        super(ELEMENT, NAMESPACE);
    }

    public IoTSetResponse(IoTSetRequest iotSetRequest) {
        this(StanzaBuilder.buildIqResultFor(iotSetRequest));
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }

}
