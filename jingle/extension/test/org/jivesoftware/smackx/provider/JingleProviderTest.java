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
package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.Jingle;

public class JingleProviderTest extends SmackTestCase {

	public JingleProviderTest(final String name) {
		super(name);
	}

	public void testProviderManager() {
		IQProvider iqProv;
		String elementNamee = Jingle.getElementName();
		String nameSpace = Jingle.getNamespace();

		System.out.println("Testing if the Jingle IQ provider is registered...");
		
		// Verify that the Jingle IQProvider is registered.
		iqProv = (IQProvider)ProviderManager.getInstance().getIQProvider(elementNamee, nameSpace);
		
		assertNotNull(iqProv);
	}
	
	/**
	 * Test for parsing a Jingle
	 */
	public void testParseIQSimple() {

		// Create a dummy packet for testing...
		IQfake iqSent = new IQfake (
				" <jingle xmlns='urn:xmpp:tmp:jingle'" +
				" initiator=\"gorrino@viejo.com\"" +
				" responder=\"colico@hepatico.com\"" +
				" action=\"transport-info\" sid=\"\">" +
				" <transport xmlns='urn:xmpp:tmp:jingle:transports:ice-udp'>" +
				" <candidate generation=\"1\"" +
				" ip=\"192.168.1.1\"" +
				" password=\"secret\"" +
				" port=\"8080\"" +
				" username=\"username\"" +
				" preference=\"1\"/>" +
				" </transport>" +
		"</jingle>");

		iqSent.setTo(getFullJID(0));
		iqSent.setFrom(getFullJID(0));
		iqSent.setType(IQ.Type.GET);

		// Create a filter and a collector...
		PacketFilter filter = new PacketTypeFilter(IQ.class);
		PacketCollector collector = getConnection(0).createPacketCollector(filter);

		System.out.println("Testing if a Jingle IQ can be sent and received...");

		// Send the iq packet with an invalid namespace
		getConnection(0).sendPacket(iqSent);

		// Receive the packet
		IQ iqReceived = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

		// Stop queuing results
		collector.cancel();

		if (iqReceived == null) {
			fail("No response from server");
		}
		else if (iqReceived.getType() == IQ.Type.ERROR) {
			fail("The server did reply with an error packet: " + iqReceived.getError().getCode());
		}
		else {
			assertTrue(iqReceived instanceof Jingle);

			Jingle jin = (Jingle) iqReceived;
			
			System.out.println("Sent:     " + iqSent.toXML());
			System.out.println("Received: " + jin.toXML());
		}		
	}

	/**
	 * Simple class for testing an IQ...
	 * @author Alvaro Saurin
	 */
	private class IQfake extends IQ {
		private String s;

		public IQfake(final String s) {
			super();
			this.s = s;
		}

		public String getChildElementXML() {
			StringBuilder buf = new StringBuilder();
			buf.append(s);
			return buf.toString();
		}
	}


	protected int getMaxConnections() {
		return 2;
	}

}
