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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.test.SmackTestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;

/** 
 * Test the PrivacyProvider class with valids privacy xmls
 * 
 * @author Francisco Vives
 */
public class PrivacyProviderTest extends SmackTestCase {

    /**
     * Constructor for PrivacyTest.
     * @param arg0
     */
    public PrivacyProviderTest(String arg0) {
        super(arg0);
    }

	public static void main(String args[]) {
		try {
			new PrivacyProviderTest(null).testFull();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

    /**
     * Check the parser with an xml with all kind of stanzas. 
     * To create the xml string based from an xml file, replace:\n with:	"\n  + "
     */
    public void testFull() {
        // Make the XML to test
    	String xml = ""
    		  + "  <iq type='result' id='getlist2' to='romeo@example.net/orchard'>	"
    		  + "  <query xmlns='jabber:iq:privacy'>	"
    		  + "    <active name='testFilter'/>	"
    		  + "    <default name='testSubscription'/>	"
    		  + "    <list name='testFilter'>	"
    		  + "      <item type='jid'	"
    		  + "            value='tybalt@example.com'	"
    		  + "            action='deny'	"
    		  + "            order='1'/>	"
    		  + "      <item action='allow' order='2'>	"
    		  + "        <message/>	"
    		  + "		<presence-in/>	"
    		  + "		<presence-out/>	"
    		  + "		<iq/>	"
    		  + "		</item>	"
    		  + "    </list>	"
    		  + "    <list name='testSubscription'>	"
    		  + "      <item type='subscription'	"
    		  + "            value='both'	"
    		  + "            action='allow'	"
    		  + "            order='10'/>	"
    		  + "      <item type='subscription'	"
    		  + "            value='to'	"
    		  + "            action='allow'	"
    		  + "            order='11'/>	"
    		  + "      <item type='subscription'	"
    		  + "            value='from'	"
    		  + "            action='allow'	"
    		  + "            order='12'/>	"
    		  + "      <item type='subscription'	"
    		  + "            value='none'	"
    		  + "            action='deny'	"
    		  + "            order='5'>	"
    		  + "        <message/>	"
    		  + "      </item>	"
    		  + "      <item action='deny' order='15'/>	"
    		  + "    </list>	"
    		  + "    <list name='testJID'>	"
    		  + "      <item type='jid'	"
    		  + "            value='juliet@example.com'	"
    		  + "            action='allow'	"
    		  + "            order='6'/>	"
    		  + "      <item type='jid'	"
    		  + "            value='benvolio@example.org/palm'	"
    		  + "            action='deny'	"
    		  + "            order='7'/>	"
    		  + "      <item type='jid'	"
    		  + "            action='allow'	"
    		  + "            order='42'/>	"
    		  + "      <item action='deny' order='666'/>	"
    		  + "    </list>	"
    		  + "    <list name='testGroup'>	"
    		  + "	      <item type='group'	"
    		  + "            value='Enemies'	"
    		  + "            action='deny'	"
    		  + "            order='4'>	"
    		  + "		   <message/>	"
    		  + "		</item>	"
    		  + "      <item action='deny' order='666'/>	"
    		  + "    </list>	"
    		  + "    <list name='testEmpty'/>	"
    		  + "  </query>	"
    		  + "  <error type='cancel'>	"
    		  + "    <item-not-found	"
    		  + "        xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>	"
    		  + "  </error>	"
    		  + "</iq>	";

        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	Privacy packet = (Privacy) (new PrivacyProvider()).parseIQ(parser);
        	
        	// check if it exist
            assertNotNull(packet);
            // assertEquals(xml, packet.getChildElementXML());
            
            // check the default and active names
            assertEquals("testFilter", packet.getActiveName());
            assertEquals("testSubscription", packet.getDefaultName());
            
            // check the list
            assertEquals(2, packet.getPrivacyList("testFilter").size());
            assertEquals(5, packet.getPrivacyList("testSubscription").size());
            assertEquals(4, packet.getPrivacyList("testJID").size());
            assertEquals(2, packet.getPrivacyList("testGroup").size());
            assertEquals(0, packet.getPrivacyList("testEmpty").size());
            
            // check each privacy item
            PrivacyItem item = packet.getItem("testGroup", 4);
            assertEquals("Enemies", item.getValue());
            assertEquals(PrivacyItem.Type.group, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(true, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(false, item.isFilterEverything());
            
            item = packet.getItem("testFilter", 1);
            assertEquals("tybalt@example.com", item.getValue());
            assertEquals(PrivacyItem.Type.jid, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testFilter", 2);
            assertEquals(null, item.getValue());
            assertEquals(null, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(true, item.isFilterMessage());
            assertEquals(true, item.isFilterIQ());
            assertEquals(true, item.isFilterPresence_in());
            assertEquals(true, item.isFilterPresence_out());
            assertEquals(false, item.isFilterEverything());

            // TEST THE testSubscription LIST
            item = packet.getItem("testSubscription", 10);
            assertEquals("both", item.getValue());
            assertEquals(PrivacyItem.Type.subscription, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());

            item = packet.getItem("testSubscription", 11);
            assertEquals("to", item.getValue());
            assertEquals(PrivacyItem.Type.subscription, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testSubscription", 12);
            assertEquals("from", item.getValue());
            assertEquals(PrivacyItem.Type.subscription, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testSubscription", 5);
            assertEquals("none", item.getValue());
            assertEquals(PrivacyItem.Type.subscription, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(true, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(false, item.isFilterEverything());
            
            item = packet.getItem("testSubscription", 15);
            assertEquals(null, item.getValue());
            assertEquals(null, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());

            // TEST THE testJID LIST
            
            item = packet.getItem("testJID", 6);
            assertEquals("juliet@example.com", item.getValue());
            assertEquals(PrivacyItem.Type.jid, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testJID", 7);
            assertEquals("benvolio@example.org/palm", item.getValue());
            assertEquals(PrivacyItem.Type.jid, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testJID", 42);
            assertEquals(null, item.getValue());
            assertEquals(PrivacyItem.Type.jid, item.getType());
            assertEquals(true, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            item = packet.getItem("testJID", 666);
            assertEquals(null, item.getValue());
            assertEquals(null, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());
            
            // TEST THE testGroup LIST
            
            item = packet.getItem("testGroup", 4);
            assertEquals("Enemies", item.getValue());
            assertEquals(PrivacyItem.Type.group, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(true, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(false, item.isFilterEverything());
            
            item = packet.getItem("testGroup", 666);
            assertEquals(null, item.getValue());
            assertEquals(null, item.getType());
            assertEquals(false, item.isAllow());
            assertEquals(false, item.isFilterMessage());
            assertEquals(false, item.isFilterIQ());
            assertEquals(false, item.isFilterPresence_in());
            assertEquals(false, item.isFilterPresence_out());
            assertEquals(true, item.isFilterEverything());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    
    /**
     * Check the parser with an xml with empty lists. It includes the active, 
     * default and special list.
     * To create the xml string based from an xml file, replace:\n with:	"\n  + "
     */
    public void testEmptyLists() {
        // Make the XML to test
    	String xml = ""
    		  + "  <iq type='result' id='getlist1' to='romeo@example.net/orchard'>	"
    		  + "  <query xmlns='jabber:iq:privacy'>	"
    		  + "    <active/>	"
    		  + "    <default name='public'/>	"
    		  + "    <list name='public'/>	"
    		  + "    <list name='private'/>	"
    		  + "    <list name='special'/>	"
    		  + "  </query>	"
    		  + " </iq>	";

        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	Privacy packet = (Privacy) (new PrivacyProvider()).parseIQ(parser);
        	
            assertNotNull(packet);
            assertNotNull(packet.getChildElementXML());
            
            assertEquals("public", packet.getDefaultName());
            assertEquals(null, packet.getActiveName());
            
            assertEquals(0, packet.getPrivacyList("public").size());
            assertEquals(0, packet.getPrivacyList("private").size());
            assertEquals(0, packet.getPrivacyList("special").size());

            assertEquals(true, packet.isDeclineActiveList());
            assertEquals(false, packet.isDeclineDefaultList());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
 
    /**
     * Check the parser with an xml with empty lists. It includes the active, 
     * default and special list.
     * To create the xml string based from an xml file, replace:\n with:	"\n  + "
     */
    public void testDeclineLists() {
        // Make the XML to test
    	String xml = ""
    		  + "  <iq type='result' id='getlist1' to='romeo@example.net/orchard'>	"
    		  + "  <query xmlns='jabber:iq:privacy'>	"
    		  + "    <active/>	"
    		  + "    <default/>	"
    		  + "  </query>	"
    		  + " </iq>	";

        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	Privacy packet = (Privacy) (new PrivacyProvider()).parseIQ(parser);
        	
            assertNotNull(packet);
            
            assertEquals(null, packet.getDefaultName());
            assertEquals(null, packet.getActiveName());
            
            assertEquals(true, packet.isDeclineActiveList());
            assertEquals(true, packet.isDeclineDefaultList());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    private XmlPullParser getParserFromXML(String xml) throws XmlPullParserException {
    	MXParser parser = new MXParser();
    	parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    	parser.setInput(new StringReader(xml));
    	return parser;
    }
    
    protected int getMaxConnections() {
        return 0;
    }
}
