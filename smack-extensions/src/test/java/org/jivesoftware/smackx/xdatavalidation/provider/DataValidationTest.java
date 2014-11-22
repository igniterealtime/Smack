/**
 *
 * Copyright 2014 Anno van Vliet
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
package org.jivesoftware.smackx.xdatavalidation.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.ListRange;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;
import org.jivesoftware.smackx.xdatavalidation.provider.DataValidationProvider.DefaultValidateElement;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * TODO add a Description.
 *
 * @author Anno van Vliet
 *
 */
public class DataValidationTest {
    private static final String TEST_OUTPUT_MIN = "<validate xmlns='http://jabber.org/protocol/xdata-validate'></validate>";
    private static final String TEST_OUTPUT_RANGE = "<validate xmlns='http://jabber.org/protocol/xdata-validate' datatype='xs:string'><range min='min-val' max='max-val'/><list-range min='111' max='999'/></validate>";
    private static final String TEST_OUTPUT_RANGE2 = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><range/><list-range/></validate>";
    private static final String TEST_OUTPUT_FAIL = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><list-range min='1-1-1' max='999'/></validate>";
    
    private static final Logger LOGGER = Logger.getLogger(DataValidationTest.class.getName());

    @Test
    public void testMin() throws XmlPullParserException, IOException, SmackException {
        
        ValidateElement dv = new DefaultValidateElement(null);
        
        assertNotNull( dv.toXML());
        String output = dv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_MIN, output);

        XmlPullParser parser = getParser(output);
        
        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue( dv instanceof BasicValidateElement);
                
        assertNotNull( dv.toXML());
        output = dv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_MIN, output);
    }

    @Test
    public void testRange() throws XmlPullParserException, IOException, SmackException {
        
        ValidateElement dv = new RangeValidateElement("xs:string", "min-val", "max-val");
        
        ListRange listRange = new ListRange(111L, 999L);
        dv.setListRange(listRange );
        
        assertNotNull( dv.toXML());
        String output = dv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_RANGE, output);

        XmlPullParser parser = getParser(output);
        
        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof RangeValidateElement );
        RangeValidateElement rdv = (RangeValidateElement) dv;
        assertEquals("min-val", rdv.getMin());
        assertEquals("max-val", rdv.getMax());
        assertNotNull(rdv.getListRange());
        assertEquals(new Long(111), rdv.getListRange().getMin());
        assertEquals(999, rdv.getListRange().getMax().intValue());
        
                
        assertNotNull( dv.toXML());
        output = dv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_RANGE, output);
    }

    @Test
    public void testRange2() throws XmlPullParserException, IOException, SmackException {
        
        ValidateElement dv = new RangeValidateElement(null, null, null);
        
        ListRange listRange = new ListRange(null, null);
        dv.setListRange(listRange );
        
        assertNotNull( dv.toXML());
        String output = dv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_RANGE2, output);

        XmlPullParser parser = getParser(output);
        
        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof RangeValidateElement );
        RangeValidateElement rdv = (RangeValidateElement) dv;
        assertEquals(null, rdv.getMin());
        assertEquals(null, rdv.getMax());
        assertNotNull(rdv.getListRange());
        assertEquals(null, rdv.getListRange().getMin());
        assertEquals(null, rdv.getListRange().getMax());
        
        assertNotNull( rdv.toXML());
        output = rdv.toXML().toString();
        LOGGER.finest(output);
        assertEquals(TEST_OUTPUT_RANGE2, output);
    }

    @Test
    public void testRangeFailure() throws IOException, SmackException, XmlPullParserException {

        try {
            XmlPullParser parser = getParser(TEST_OUTPUT_FAIL);
            
            ValidateElement dv = DataValidationProvider.parse(parser);
            
            fail("Exception should occur");
            assertNull(dv);

        }
        catch (NumberFormatException e) {
            //Success
        }
    }

    
    /**
     * @param output
     * @return
     * @throws XmlPullParserException 
     * @throws IOException 
     */
    private XmlPullParser getParser(String output) throws XmlPullParserException, IOException {
        
        return TestUtils.getParser(output, "validate");
    }
}
