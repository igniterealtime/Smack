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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.ListRange;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Data validation test.
 * @author Anno van Vliet
 *
 */
public class DataValidationTest {
    private static final String TEST_INPUT_MIN = "<validate xmlns='http://jabber.org/protocol/xdata-validate'></validate>";
    private static final String TEST_OUTPUT_MIN = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><basic/></validate>";
    private static final String TEST_OUTPUT_RANGE = "<validate xmlns='http://jabber.org/protocol/xdata-validate' datatype='xs:string'><range min='min-val' max='max-val'/><list-range min='111' max='999'/></validate>";
    private static final String TEST_OUTPUT_RANGE2 = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><range/></validate>";
    private static final String TEST_OUTPUT_FAIL = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><list-range min='1-1-1' max='999'/></validate>";

    @Test
    public void testMin() throws XmlPullParserException, IOException {

        ValidateElement dv = new BasicValidateElement(null);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_MIN, output);

        XmlPullParser parser = getParser(TEST_INPUT_MIN);

        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof BasicValidateElement);

        assertNotNull(dv.toXML());
        output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_MIN, output);
    }

    @Test
    public void testRange() throws XmlPullParserException, IOException {

        ValidateElement dv = new RangeValidateElement("xs:string", "min-val", "max-val");

        ListRange listRange = new ListRange(111L, 999L);
        dv.setListRange(listRange);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE, output);

        XmlPullParser parser = getParser(output);

        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof RangeValidateElement);
        RangeValidateElement rdv = (RangeValidateElement) dv;
        assertEquals("min-val", rdv.getMin());
        assertEquals("max-val", rdv.getMax());
        assertNotNull(rdv.getListRange());
        assertEquals(Long.valueOf(111), rdv.getListRange().getMin());
        assertEquals(999, rdv.getListRange().getMax().intValue());


        assertNotNull(dv.toXML());
        output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE, output);
    }

    @Test
    public void testRange2() throws XmlPullParserException, IOException {

        ValidateElement dv = new RangeValidateElement(null, null, null);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE2, output);

        XmlPullParser parser = getParser(output);

        dv = DataValidationProvider.parse(parser);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof RangeValidateElement);
        RangeValidateElement rdv = (RangeValidateElement) dv;
        assertEquals(null, rdv.getMin());
        assertEquals(null, rdv.getMax());

        assertNotNull(rdv.toXML());
        output = rdv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE2, output);
    }

    @Test(expected=NumberFormatException.class)
    public void testRangeFailure() throws IOException, XmlPullParserException {
            XmlPullParser parser = getParser(TEST_OUTPUT_FAIL);
            DataValidationProvider.parse(parser);
    }

    /**
     * @param output
     * @return
     * @throws XmlPullParserException 
     * @throws IOException 
     */
    private static XmlPullParser getParser(String output) throws XmlPullParserException, IOException {
        return TestUtils.getParser(output, "validate");
    }
}
