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
package org.jivesoftware.smackx.iot.data.element;

import org.jivesoftware.smack.packet.IQ;

public class IoTDataReadOutAccepted extends IQ {

    public static final String ELEMENT = "accepted";
    public static final String NAMESPACE = Constants.IOT_SENSORDATA_NAMESPACE;

    /**
     * The sequence number. According to XEP-0323 an xs:int.
     */
    private final int seqNr;

    private final boolean queued;

    public IoTDataReadOutAccepted(int seqNr, boolean queued) {
        super(ELEMENT, NAMESPACE);
        this.seqNr = seqNr;
        this.queued = queued;
        setType(Type.result);
    }

    public IoTDataReadOutAccepted(IoTDataRequest dataRequest) {
        this(dataRequest.getSequenceNr(), false);
        setStanzaId(dataRequest.getStanzaId());
        setTo(dataRequest.getFrom());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("seqnr", seqNr);
        xml.optBooleanAttribute("queued", queued);
        xml.setEmptyElement();
        return xml;
    }

}
