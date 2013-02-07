/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
package org.jivesoftware.smackx.pubsub;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Reader;
import java.io.StringReader;

import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smackx.pubsub.provider.ItemsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

public class ItemValidationTest
{
	private ThreadedDummyConnection connection;

	@Before
	public void setUp() throws Exception
	{
		// Uncomment this to enable debug output
		// Connection.DEBUG_ENABLED = true;

		connection = new ThreadedDummyConnection();
		connection.connect();
		connection.login("me", "secret");
	}

	@After
	public void tearDown() throws Exception
	{
		if (connection != null)
			connection.disconnect();
	}

	@Test
	public void verifyBasicItem() throws Exception
	{
		Item simpleItem = new Item();
		String simpleCtrl = "<item />";
		assertXMLEqual(simpleCtrl, simpleItem.toXML());

		Item idItem = new Item("uniqueid");
		String idCtrl = "<item id='uniqueid'/>";
		assertXMLEqual(idCtrl, idItem.toXML());

		Item itemWithNodeId = new Item("testId", "testNode");
		String nodeIdCtrl = "<item id='testId' node='testNode' />";
		assertXMLEqual(nodeIdCtrl, itemWithNodeId.toXML());
	}

	@Test
	public void verifyPayloadItem() throws Exception
	{
		SimplePayload payload = new SimplePayload(null, null, "<data>This is the payload</data>");

		PayloadItem<SimplePayload> simpleItem = new PayloadItem<SimplePayload>(payload);
		String simpleCtrl = "<item>" + payload.toXML() + "</item>";
		assertXMLEqual(simpleCtrl, simpleItem.toXML());

		PayloadItem<SimplePayload> idItem = new PayloadItem<SimplePayload>("uniqueid", payload);
		String idCtrl = "<item id='uniqueid'>" + payload.toXML() + "</item>";
		assertXMLEqual(idCtrl, idItem.toXML());

		PayloadItem<SimplePayload> itemWithNodeId = new PayloadItem<SimplePayload>("testId", "testNode", payload);
		String nodeIdCtrl = "<item id='testId' node='testNode'>" + payload.toXML() + "</item>";
		assertXMLEqual(nodeIdCtrl, itemWithNodeId.toXML());
	}

//	@Test
//	public void parseBasicItemWithoutNode() throws Exception
//	{
//		XmlPullParser parser = new MXParser();
//		Reader reader = new StringReader(
//				"<event xmlns='http://jabber.org/protocol/pubsub#event'>" +
//				"<items node='testNode'>" +
//					"<item id='testid1' />" +
//				"</items></event>");
//		parser.setInput(reader);
//		ItemsProvider itemsProvider = new ItemsProvider();
//		ItemsExtension ext = (ItemsExtension) itemsProvider.parseExtension(parser);
//		Item basicItem = (Item) ext.getItems().get(0);
//
//		assertEquals("testid1", basicItem.getId());
//		assertNull(basicItem.getNode());
//	}

//	@Test
//	public void parseBasicItemNode() throws Exception
//	{
//		BlockingQueue<Item> itemQ = new ArrayBlockingQueue<Item>(1);
//
//		setupListener(itemQ);
//		Message itemMsg = getMessage("<item id='testid1' node='testNode'>");
//		connection.addMessage(itemMsg);
//		
//		Item basicItem = itemQ.poll(2, TimeUnit.SECONDS);
//		
//		assertNotNull(basicItem);
//		assertEquals("testid1", basicItem.getId());
//		assertEquals("testNode", basicItem.getNode());
//	}
}
