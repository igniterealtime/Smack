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

/**
 * MUC Light room configuration class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightRoomConfiguration {

    private final String roomName;
    private final String subject;
    private final HashMap<String, String> customConfigs;

    /**
     * MUC Light room configuration model constructor.
     *
     * @param roomName
     * @param subject
     * @param customConfigs
     */
    public MUCLightRoomConfiguration(String roomName, String subject, HashMap<String, String> customConfigs) {
        this.roomName = roomName;
        this.subject = subject;
        this.customConfigs = customConfigs;
    }

    /**
     * Returns the room name.
     *
     * @return the name of the room.
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Returns the room subject.
     *
     * @return the subject of the room.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the room custom configurations.
     *
     * @return the custom configurations of the room.
     */
    public HashMap<String, String> getCustomConfigs() {
        return customConfigs;
    }

}
