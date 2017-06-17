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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.muclight.MultiUserChatLight;

import org.jxmpp.jid.Jid;

/**
 * MUC Light set configurations IQ class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightSetConfigsIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.CONFIGURATION;

    private String roomName;
    private String subject;
    private HashMap<String, String> customConfigs;

    /**
     * MUC Light set configuration IQ constructor.
     * 
     * @param roomJid
     * @param roomName
     * @param subject
     * @param customConfigs
     */
    public MUCLightSetConfigsIQ(Jid roomJid, String roomName, String subject, HashMap<String, String> customConfigs) {
        super(ELEMENT, NAMESPACE);
        this.roomName = roomName;
        this.subject = subject;
        this.customConfigs = customConfigs;
        this.setType(Type.set);
        this.setTo(roomJid);
    }

    /**
     * MUC Light set configuration IQ constructor.
     * 
     * @param roomJid
     * @param roomName
     * @param customConfigs
     */
    public MUCLightSetConfigsIQ(Jid roomJid, String roomName, HashMap<String, String> customConfigs) {
        this(roomJid, roomName, null, customConfigs);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("roomname", roomName);
        xml.optElement("subject", subject);

        if (customConfigs != null) {
            Iterator<Map.Entry<String, String>> it = customConfigs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();
                xml.element(pair.getKey(), pair.getValue());
            }
        }

        return xml;
    }

}
