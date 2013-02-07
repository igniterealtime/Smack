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
package org.jivesoftware.smack.util;

import java.io.StringReader;

import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.test.SmackTestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XMPPErrorTest extends SmackTestCase {

    public XMPPErrorTest(String arg0) {
        super(arg0);
    }
	
    /**
     * Check the creation of a new xmppError locally.
    */
    public void testLocalErrorCreation() {
    	XMPPError error = new XMPPError(XMPPError.Condition.item_not_found);
        error.toXML();

    	assertEquals(error.getCondition(), "item-not-found");
    	assertEquals(error.getCode(), 404);
    	assertEquals(error.getType(), XMPPError.Type.CANCEL);
    	assertNull(error.getMessage());
    }
 
    /**
     * Check the creation of a new xmppError locally.
    */
    public void testLocalErrorWithCommentCreation() {
        String message = "Error Message";
        XMPPError error = new XMPPError(XMPPError.Condition.item_not_found, message);
        error.toXML();

        assertEquals(error.getCondition(), "item-not-found");
        assertEquals(error.getCode(), 404);
        assertEquals(error.getType(), XMPPError.Type.CANCEL);
        assertEquals(error.getMessage(), message);
    }
    
    /**
     * Check the creation of a new xmppError locally where there is not a default defined.
    */
    public void testUserDefinedErrorWithCommentCreation() {
        String message = "Error Message";
        XMPPError error = new XMPPError(new XMPPError.Condition("my_own_error"), message);
        error.toXML();

        assertEquals(error.getCondition(), "my_own_error");
        assertEquals(error.getCode(), 0);
        assertNull(error.getType());
        assertEquals(error.getMessage(), message);
    }
    
    /**
     * Check the parser with an xml with the 404 error.
    */
    public void test404() {
        // Make the XML to test
    	String xml = "<error code='404' type='cancel'>" +
    			"<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
    			"</error></iq>";
        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	XMPPError packet = parseError(parser);
        	
            assertNotNull(packet);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
 
    /**
     * Check the parser with an xml with the 404 error.
    */
    public void testCancel() {
        // Make the XML to test
    	String xml = "<error type='cancel'>" +
    			"<conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
    			"</error>";
        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	XMPPError error = parseError(parser);
        	
            assertNotNull(error);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
   public void testMessageAndApplicationDefinedError() {
	   String xml = "<error type='modify' code='404'>" +
	   		"<undefined-condition xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
	   		"<text xml:lang='en' xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'>" +
	   		"Some special application diagnostic information..." +
	   		"</text>" +
	   		"<special-application-condition xmlns='application-ns'/>" +
	   		"</error>";
       try {
       	// Create the xml parser
       	XmlPullParser parser = getParserFromXML(xml);
       	// Create a packet from the xml
       	XMPPError error = parseError(parser);
       	
       	String sendingXML = error.toXML();
       	
       assertNotNull(error);
       assertNotNull(sendingXML);
       } catch (Exception e) {
           e.printStackTrace();
           fail(e.getMessage());
       }
   }
    /**
     * Check the parser with an xml with the 404 error.
    */
    public void testCancelWithMessage() {
        // Make the XML to test
    	String xml = "<error type='cancel'>" +
    			"<conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
    			"<text xmlns='urn:ietf:params:xml:ns:xmpp-stanzas' xml:lang='langcode'>" +
    			"Some special application diagnostic information!" +
    			"</text>" +
    			"</error>";
        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	XMPPError error = parseError(parser);
        	
            assertNotNull(error);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
 
    /**
     * Check the parser with an xml with the 404 error.
    */
    public void testCancelWithMessageAndApplicationError() {
        // Make the XML to test
    	String xml = "<error type='cancel' code='10'>" +
    			"<conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
    			"<text xml:lang='en' xmlns='urn:ietf:params:xml:ns:xmpp-streams'>" +
    			"Some special application diagnostic information!" +
    			"</text>" +
    			"<application-defined-error xmlns='application-ns'/>" +
    			"</error>";
        try {
        	// Create the xml parser
        	XmlPullParser parser = getParserFromXML(xml);
        	// Create a packet from the xml
        	XMPPError error = parseError(parser);
        	
            assertNotNull(error);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    private XMPPError parseError(XmlPullParser parser) throws Exception {
    	parser.next();
    	return PacketParserUtils.parseError(parser);
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
