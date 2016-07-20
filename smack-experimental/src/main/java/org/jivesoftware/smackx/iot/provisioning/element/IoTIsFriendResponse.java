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
package org.jivesoftware.smackx.iot.provisioning.element;

import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.BareJid;

public class IoTIsFriendResponse extends IQ {

    public static final String ELEMENT = "isFriend";
    public static final String NAMESPACE = Constants.IOT_PROVISIONING_NAMESPACE;

    private final BareJid jid;

    private final boolean result;

    public IoTIsFriendResponse(BareJid jid, boolean result) {
        super(ELEMENT, NAMESPACE);
        this.jid = jid;
        this.result = result;
    }

    public BareJid getJid() {
        return jid;
    }

    public boolean getIsFriendResult() {
        return result;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("jid", jid);
        xml.attribute("result", result);
        return xml;
    }

}
