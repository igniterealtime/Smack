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
 * MUC Light destroy IQ class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightDestroyIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.DESTROY;

    /**
     * MUC Light destroy IQ constructor.
     * 
     * @param roomJid
     */
    public MUCLightDestroyIQ(Jid roomJid) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setTo(roomJid);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }

}
