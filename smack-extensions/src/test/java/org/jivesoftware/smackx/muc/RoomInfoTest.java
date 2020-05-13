/**
 *
 * Copyright 2011 Robin Collier
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
package org.jivesoftware.smackx.muc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.Test;

public class RoomInfoTest {
    @Test
    public void validateRoomWithEmptyForm() {
        DataForm dataForm = DataForm.builder(DataForm.Type.result).build();

        DiscoverInfo discoInfo = DiscoverInfo.builder("disco1")
                .addExtension(dataForm)
                .build();
        RoomInfo roomInfo = new RoomInfo(discoInfo);
        assertTrue(roomInfo.getDescription().isEmpty());
        assertTrue(roomInfo.getSubject().isEmpty());
        assertEquals(-1, roomInfo.getOccupantsCount());
    }

    @Test
    public void validateRoomWithForm() {
        DataForm.Builder dataForm = DataForm.builder(DataForm.Type.result);

        TextSingleFormField.Builder desc = FormField.builder("muc#roominfo_description");
        desc.setValue("The place for all good witches!");
        dataForm.addField(desc.build());

        TextSingleFormField.Builder subject = FormField.builder("muc#roominfo_subject");
        subject.setValue("Spells");
        dataForm.addField(subject.build());

        TextSingleFormField.Builder occupants = FormField.builder("muc#roominfo_occupants");
        occupants.setValue("3");
        dataForm.addField(occupants.build());

        DiscoverInfo discoInfo = DiscoverInfo.builder("disco1")
                .addExtension(dataForm.build())
                .build();
        RoomInfo roomInfo = new RoomInfo(discoInfo);
        assertEquals("The place for all good witches!", roomInfo.getDescription());
        assertEquals("Spells", roomInfo.getSubject());
        assertEquals(3, roomInfo.getOccupantsCount());
    }
}
