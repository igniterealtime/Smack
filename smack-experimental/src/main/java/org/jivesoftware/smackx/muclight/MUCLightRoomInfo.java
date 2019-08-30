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
package org.jivesoftware.smackx.muclight;

import java.util.HashMap;

import org.jxmpp.jid.Jid;

/**
 * MUC Light room info class.
 *
 * @author Fernando Ramirez
 */
public class MUCLightRoomInfo {

    private final String version;
    private final Jid room;
    private final MUCLightRoomConfiguration configuration;
    private final HashMap<Jid, MUCLightAffiliation> occupants;

    /**
     * MUC Light room info model constructor.
     *
     * @param version TODO javadoc me please
     * @param roomJid TODO javadoc me please
     * @param configuration TODO javadoc me please
     * @param occupants TODO javadoc me please
     */
    public MUCLightRoomInfo(String version, Jid roomJid, MUCLightRoomConfiguration configuration,
            HashMap<Jid, MUCLightAffiliation> occupants) {
        this.version = version;
        this.room = roomJid;
        this.configuration = configuration;
        this.occupants = occupants;
    }

    /**
     * Returns the version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the JID of the room whose information was discovered.
     *
     * @return the JID of the room whose information was discovered.
     */
    public Jid getRoom() {
        return room;
    }

    /**
     * Returns the configuration.
     *
     * @return the room configuration
     */
    public MUCLightRoomConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the room occupants.
     *
     * @return the occupants of the room.
     */
    public HashMap<Jid, MUCLightAffiliation> getOccupants() {
        return occupants;
    }

}
