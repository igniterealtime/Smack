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
package org.jivesoftware.smackx.xdata.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Fieldref;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Section;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Text;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Unit tests for DataForm reading and parsing.
 *
 * @author Anno van Vliet
 *
 */
public class DataFormTest {
    private static final String TEST_OUTPUT_1 = "<x xmlns='jabber:x:data' type='SUBMIT'><instructions>InstructionTest1</instructions><field var='testField1'></field></x>";
    private static final String TEST_OUTPUT_2 = "<x xmlns='jabber:x:data' type='SUBMIT'><instructions>InstructionTest1</instructions><field var='testField1'></field><page xmlns='http://jabber.org/protocol/xdata-layout' label='Label'><fieldref var='testField1'/><section label='section Label'><text>SectionText</text></section><text>PageText</text></page></x>";
    private static Logger logger = Logger.getLogger(DataFormTest.class.getName());

    @Test
    public void test() throws XmlPullParserException, IOException, SmackException {
        
        //Build a Form
        DataForm df = new DataForm("SUBMIT");
        String instruction = "InstructionTest1";
        df.addInstruction(instruction);
        FormField field = new FormField("testField1");
        df.addField(field);
        
        assertNotNull( df.toXML());
        String output = df.toXML().toString();
        logger.finest(output);
        assertEquals(TEST_OUTPUT_1, output);
        
        DataFormProvider pr = new DataFormProvider();
        
        XmlPullParser parser = getParser(output);
        
        df = pr.parse(parser);
        
        assertNotNull(df);
        assertNotNull(df.getFields());
        assertEquals(1 , df.getFields().size() );
        assertEquals(1 , df.getInstructions().size());

        assertNotNull( df.toXML());
        output = df.toXML().toString();
        logger.finest(output);
        assertEquals(TEST_OUTPUT_1, output);

        
    }

    @Test
    public void testLayout() throws XmlPullParserException, IOException, SmackException {
        
        //Build a Form
        DataForm df = new DataForm("SUBMIT");
        String instruction = "InstructionTest1";
        df.addInstruction(instruction);
        FormField field = new FormField("testField1");
        df.addField(field);
        
        DataLayout layout = new DataLayout("Label");
        Fieldref reffield = new Fieldref("testField1");
        layout.getPageLayout().add(reffield);
        Section section = new Section("section Label");
        section.getSectionLayout().add(new Text("SectionText"));
        layout.getPageLayout().add(section);
        layout.getPageLayout().add(new Text("PageText"));
        
        df.addExtensionElement(layout);
        
        
        assertNotNull( df.toXML());
        String output = df.toXML().toString();
        logger.finest(output);
        assertEquals(TEST_OUTPUT_2, output);
        
        DataFormProvider pr = new DataFormProvider();
        
        XmlPullParser parser = getParser(output);
        
        df = pr.parse(parser);
        
        assertNotNull(df);
        assertNotNull(df.getExtensionElements());
        assertEquals(1 , df.getExtensionElements().size() );
        Element element = df.getExtensionElements().get(0);
        assertNotNull(element);
        layout = (DataLayout) element;
        
        assertEquals(3 , layout.getPageLayout().size());

        assertNotNull( df.toXML());
        output = df.toXML().toString();
        logger.finest(output);
        assertEquals(TEST_OUTPUT_2, output);

        
    }

    /**
     * @param output
     * @return
     * @throws XmlPullParserException 
     * @throws IOException 
     */
    private XmlPullParser getParser(String output) throws XmlPullParserException, IOException {
        logger.finest("getParser");
        XmlPullParser parser = PacketParserUtils.newXmppParser();
        parser.setInput(new StringReader(output));
        parser.next();
        return parser;
    }
}
