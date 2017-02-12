/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.ox.element;

import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

public abstract class EncryptedOpenPgpContentElement extends OpenPgpContentElement {

    private final String rpad;

    protected EncryptedOpenPgpContentElement(List<Jid> to, String rpad, Date timestamp, List<ExtensionElement> payload) {
        super(to, timestamp, payload);
        this.rpad = rpad;
    }

    @Override
    protected void addCommonXml(XmlStringBuilder xml) {
        super.addCommonXml(xml);

        xml.openElement("rpad").escape(rpad).closeElement("rpad");
    }
}
