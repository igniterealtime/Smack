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
