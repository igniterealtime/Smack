/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RoomInfoTest 
{
    @Test
    public void validateRoomWithEmptyForm() 
    {
	DataForm dataForm = new DataForm("result");
	
	DiscoverInfo discoInfo = new DiscoverInfo();
	discoInfo.addExtension(dataForm);
	RoomInfo roomInfo = new RoomInfo(discoInfo);
	assertTrue(roomInfo.getDescription().isEmpty());
	assertTrue(roomInfo.getSubject().isEmpty());
	assertEquals(-1, roomInfo.getOccupantsCount());
    }

    @Test
    public void validateRoomWithForm() 
    {
	DataForm dataForm = new DataForm("result");
	
	FormField desc = new FormField("muc#roominfo_description");
	desc.addValue("The place for all good witches!");
	dataForm.addField(desc);

	FormField subject = new FormField("muc#roominfo_subject");
	subject.addValue("Spells");
	dataForm.addField(subject);

	FormField occupants = new FormField("muc#roominfo_occupants");
	occupants.addValue("3");
	dataForm.addField(occupants);

	DiscoverInfo discoInfo = new DiscoverInfo();
	discoInfo.addExtension(dataForm);
	RoomInfo roomInfo = new RoomInfo(discoInfo);
	assertEquals("The place for all good witches!", roomInfo.getDescription());
	assertEquals("Spells", roomInfo.getSubject());
	assertEquals(3, roomInfo.getOccupantsCount());
    }
}
