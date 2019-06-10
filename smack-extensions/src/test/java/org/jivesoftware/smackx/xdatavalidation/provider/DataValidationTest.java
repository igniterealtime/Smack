/**
 *
 * Copyright 2014 Anno van Vliet, 2018-2019 Florian Schmaus
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.BasicValidateElement;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.ListRange;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement.RangeValidateElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Data validation test.
 * @author Anno van Vliet
 *
 */
public class DataValidationTest extends SmackTestSuite {
    private static final String TEST_INPUT_MIN = "<validate xmlns='http://jabber.org/protocol/xdata-validate'></validate>";
    private static final String TEST_OUTPUT_MIN = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><basic/></validate>";
    private static final String TEST_OUTPUT_RANGE = "<validate xmlns='http://jabber.org/protocol/xdata-validate' datatype='xs:string'><range min='min-val' max='max-val'/><list-range min='111' max='999'/></validate>";
    private static final String TEST_OUTPUT_RANGE2 = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><range/></validate>";
    private static final String TEST_OUTPUT_FAIL = "<validate xmlns='http://jabber.org/protocol/xdata-validate'><list-range min='1-1-1' max='999'/></validate>";

    @Test
    public void testBasic() {
        ValidateElement dv = new BasicValidateElement(null);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_MIN, output);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testMin(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        ValidateElement dv = SmackTestUtil.parse(TEST_INPUT_MIN, DataValidationProvider.class, parserKind);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof BasicValidateElement);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_MIN, output);
    }

    @Test
    public void testRangeToXml() {
        ValidateElement dv = new RangeValidateElement("xs:string", "min-val", "max-val");
        ListRange listRange = new ListRange(111L, 999L);
        dv.setListRange(listRange);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE, output);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testRange(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        ValidateElement dv = new RangeValidateElement("xs:string", "min-val", "max-val");
        ListRange listRange = new ListRange(111L, 999L);
        dv.setListRange(listRange);

        String xml = dv.toXML().toString();

        dv = SmackTestUtil.parse(xml, DataValidationProvider.class, parserKind);

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
        xml = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE, xml);
    }

    @Test
    public void testRange2ToXml() {
        ValidateElement dv = new RangeValidateElement(null, null, null);

        assertNotNull(dv.toXML());
        String output = dv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE2, output);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testRange2(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        ValidateElement dv = new RangeValidateElement(null, null, null);
        String xml = dv.toXML().toString();

        dv = SmackTestUtil.parse(xml, DataValidationProvider.class, parserKind);

        assertNotNull(dv);
        assertEquals("xs:string", dv.getDatatype());
        assertTrue(dv instanceof RangeValidateElement);
        RangeValidateElement rdv = (RangeValidateElement) dv;
        assertEquals(null, rdv.getMin());
        assertEquals(null, rdv.getMax());

        assertNotNull(rdv.toXML());
        xml = rdv.toXML().toString();
        assertEquals(TEST_OUTPUT_RANGE2, xml);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testRangeFailure(SmackTestUtil.XmlPullParserKind parserKind) throws IOException, XmlPullParserException {
        assertThrows(NumberFormatException.class,
                        () -> SmackTestUtil.parse(TEST_OUTPUT_FAIL, DataValidationProvider.class, parserKind));
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testNamespacePrefix(SmackTestUtil.XmlPullParserKind parserKind) throws Exception {
        String formFieldUsingNamespacePrefix =
                "<x xmlns='jabber:x:data'" +
                "   xmlns:xdv='http://jabber.org/protocol/xdata-validate'" +
                "   type='form'>" +
                "  <title>Sample Form</title>" +
                "  <instructions>" +
                "    Please provide information for the following fields..." +
                "  </instructions>" +
                "  <field type='text-single' var='name' label='Event Name'/>" +
                "  <field type='text-single' var='date/start' label='Starting Date'>" +
                "    <xdv:validate datatype='xs:date'>" +
                "      <basic/>" +
                "    </xdv:validate>" +
                "  </field>" +
                "  <field type='text-single' var='date/end' label='Ending Date'>" +
                "    <xdv:validate datatype='xs:date'>" +
                "      <basic/>" +
                "    </xdv:validate>" +
                "  </field>" +
                "</x>";

        DataForm dataForm = SmackTestUtil.parse(formFieldUsingNamespacePrefix, DataFormProvider.class, parserKind);

        assertEquals("Sample Form", dataForm.getTitle());

        FormField nameField = dataForm.getField("name");
        assertEquals("Event Name", nameField.getLabel());

        FormField dataStartField = dataForm.getField("date/start");
        ValidateElement dataStartValidateElement = ValidateElement.from(dataStartField);
        assertEquals("xs:date", dataStartValidateElement.getDatatype());
        assertTrue(dataStartValidateElement instanceof BasicValidateElement);
    }
}
