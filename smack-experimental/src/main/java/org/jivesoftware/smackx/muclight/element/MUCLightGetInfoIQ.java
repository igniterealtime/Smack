/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.muclight.element;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.muclight.MultiUserChatLight;

import org.jxmpp.jid.Jid;

/**
 * MUC Light get info IQ class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightGetInfoIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.INFO;

    private String version;

    /**
     * MUC Light get info IQ constructor.
     *
     * @param roomJid
     * @param version
     */
    public MUCLightGetInfoIQ(Jid roomJid, String version) {
        super(ELEMENT, NAMESPACE);
        this.version = version;
        this.setType(Type.get);
        this.setTo(roomJid);
    }

    /**
     * MUC Light get info IQ constructor.
     *
     * @param roomJid
     */
    public MUCLightGetInfoIQ(Jid roomJid) {
        this(roomJid, null);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("version", version);
        return xml;
    }

}
