/*
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
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.ConfigurationElement;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.OccupantsElement;

import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;

/**
 * MUCLight create IQ class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightCreateIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.CREATE;

    private MUCLightRoomConfiguration configuration;
    private final Map<Jid, MUCLightAffiliation> occupants;

    /**
     * MUCLight create IQ constructor.
     *
     * @param room TODO javadoc me please
     * @param roomName TODO javadoc me please
     * @param subject TODO javadoc me please
     * @param customConfigs TODO javadoc me please
     * @param occupants TODO javadoc me please
     */
    @SuppressWarnings("this-escape")
    public MUCLightCreateIQ(EntityJid room, String roomName, String subject, Map<String, String> customConfigs,
            List<Jid> occupants) {
        super(ELEMENT, NAMESPACE);
        this.configuration = new MUCLightRoomConfiguration(roomName, subject, customConfigs);

        this.occupants = new HashMap<>();
        for (Jid occupant : occupants) {
            this.occupants.put(occupant, MUCLightAffiliation.member);
        }

        this.setType(Type.set);
        this.setTo(room);
    }

    /**
     * MUCLight create IQ constructor.
     *
     * @param room TODO javadoc me please
     * @param roomName TODO javadoc me please
     * @param occupants TODO javadoc me please
     */
    public MUCLightCreateIQ(EntityJid room, String roomName, List<Jid> occupants) {
        this(room, roomName, null, null, occupants);
    }

    /**
     * Get the room configuration.
     *
     * @return the room configuration
     */
    public MUCLightRoomConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get the room occupants.
     *
     * @return the room occupants
     */
    public Map<Jid, MUCLightAffiliation> getOccupants() {
        return occupants;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.append(new ConfigurationElement(configuration));

        if (!occupants.isEmpty()) {
            xml.append(new OccupantsElement(occupants));
        }

        return xml;
    }

}
