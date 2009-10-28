/*
 * Created on 2009-08-05
 */
package org.jivesoftware.smackx.pubsub;

import junit.framework.TestCase;

public class TestMessageContent extends TestCase
{
	String payloadXmlWithNS = "<book xmlns='pubsub:test:book'><author name='Stephen King'/></book>";
	
	public void testItemWithId() 
	{
		Item item = new Item("123");
		assertEquals("<item id='123'/>", item.toXML());
		assertEquals("item", item.getElementName());
		assertNull(item.getNamespace());
	}

	public void testItemWithNoId() 
	{
		Item item = new Item();
		assertEquals("<item/>", item.toXML());

		Item itemNull = new Item(null);
		assertEquals("<item/>", itemNull.toXML());
	}

	public void testSimplePayload() 
	{
		SimplePayload payloadNS = new SimplePayload("book", "pubsub:test:book", payloadXmlWithNS);
		
		assertEquals(payloadXmlWithNS, payloadNS.toXML());
		
		String payloadXmlWithNoNS = "<book><author name='Stephen King'/></book>";
		SimplePayload payloadNoNS = new SimplePayload("book", null, "<book><author name='Stephen King'/></book>");
		
		assertEquals(payloadXmlWithNoNS, payloadNoNS.toXML());
		
	}
	
	public void testPayloadItemWithId()
	{
		SimplePayload payload = new SimplePayload("book", "pubsub:test:book", payloadXmlWithNS);
		PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>("123", payload);
		
		String xml = "<item id='123'>" + payloadXmlWithNS + "</item>";
		assertEquals(xml, item.toXML());
		assertEquals("item", item.getElementName());
	}

	public void testPayloadItemWithNoId()
	{
		SimplePayload payload = new SimplePayload("book", "pubsub:test:book", payloadXmlWithNS);
		PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(null, payload);
		
		String xml = "<item>" + payloadXmlWithNS + "</item>";
		assertEquals(xml, item.toXML());
	}

	public void testPayloadItemWithIdNoPayload()
	{
		try
		{
			PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>("123", null);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
		}
	}

	public void testPayloadItemWithNoIdNoPayload()
	{
		try
		{
			PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(null, null);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
		}
	}
	
	public void testRetractItem()
	{
		RetractItem item = new RetractItem("1234");
		
		assertEquals("<retract id='1234'/>", item.toXML());
		assertEquals("retract", item.getElementName());
		
		try
		{
			new RetractItem(null);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException e)
		{
		}
	}
}
