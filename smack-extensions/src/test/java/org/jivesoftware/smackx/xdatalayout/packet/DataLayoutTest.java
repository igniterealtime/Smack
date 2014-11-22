/**
 *
 * Copyright 2014 Anno van Vliet, All rights reserved. 
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
package org.jivesoftware.smackx.xdatalayout.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStreamReader;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Fieldref;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Reportedref;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Section;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Text;
import org.jivesoftware.smackx.xdatalayout.provider.DataLayoutProvider;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Unit tests for DataForm reading and parsing.
 *
 * @author Anno van Vliet
 *
 */
public class DataLayoutTest {
    private static final String TEST_OUTPUT_2 = "<page xmlns='http://jabber.org/protocol/xdata-layout' label='Label'><fieldref var='testField1'/><section label='section Label'><text>SectionText</text></section><text>PageText</text></page>";
    private static final String TEST_OUTPUT_SPECIAL = "<page xmlns='http://jabber.org/protocol/xdata-layout' label='Label - &amp; \u00E9 \u00E1 '><fieldref var='testField1'/><section label='section Label - &amp; \u00E9 \u00E1 '><text>SectionText - &amp; \u00E9 \u00E1 </text></section><text>PageText - &amp; \u00E9 \u00E1 </text><section label='&lt;html&gt;Number of Persons by&lt;br/&gt; Nationality and Status&lt;/html&gt;'><reportedref/></section><text>&lt;html&gt;&lt;font color=&apos;red&apos;&gt;&lt;em&gt;DO NOT DELAY&lt;/em&gt;&lt;/font&gt;&lt;br/&gt;supply further information&lt;/html&gt;</text></page>";
    private static final String TEST_INPUT_1 = "xdata-layout-sample.xml";

    @Test
    public void testLayout() throws XmlPullParserException, IOException, SmackException {
        DataLayout layout = new DataLayout("Label");
        Fieldref reffield = new Fieldref("testField1");
        layout.getPageLayout().add(reffield);
        Section section = new Section("section Label");
        section.getSectionLayout().add(new Text("SectionText"));
        layout.getPageLayout().add(section);
        layout.getPageLayout().add(new Text( "PageText"));
        
        assertNotNull( layout.toXML());
        String output = layout.toXML().toString();
        assertEquals(TEST_OUTPUT_2, output);
        
        XmlPullParser parser = PacketParserUtils.getParserFor(output);
        
        layout = DataLayoutProvider.parse(parser);
        
        assertEquals(3 , layout.getPageLayout().size());
        assertEquals("Label", layout.getLabel());

        assertNotNull( layout.toXML());
        output = layout.toXML().toString();
        assertEquals(TEST_OUTPUT_2, output);
    }

    @Test
    public void testLayoutSpecialCharacters() throws XmlPullParserException, IOException, SmackException {
        
        DataLayout layout = new DataLayout("Label - & \u00E9 \u00E1 ");
        Fieldref reffield = new Fieldref("testField1");
        layout.getPageLayout().add(reffield);
        Section section = new Section("section Label - & \u00E9 \u00E1 ");
        section.getSectionLayout().add(new Text( "SectionText - & \u00E9 \u00E1 "));
        layout.getPageLayout().add(section);
        layout.getPageLayout().add(new Text( "PageText - & \u00E9 \u00E1 "));
        
        section = new Section("<html>Number of Persons by<br/> Nationality and Status</html>");
        section.getSectionLayout().add(new Reportedref());
        layout.getPageLayout().add(section);

        layout.getPageLayout().add(new Text( "<html><font color='red'><em>DO NOT DELAY</em></font><br/>supply further information</html>"));

        
        assertNotNull( layout.toXML());
        String output = layout.toXML().toString();
        assertEquals(TEST_OUTPUT_SPECIAL, output);
        
        XmlPullParser parser = PacketParserUtils.getParserFor(output);
        
        layout = DataLayoutProvider.parse(parser);
        
        assertEquals(5 , layout.getPageLayout().size());
        assertEquals("Label - & \u00E9 \u00E1 ", layout.getLabel());
        section = (Section) layout.getPageLayout().get(1);
        assertEquals("section Label - & \u00E9 \u00E1 ", section.getLabel());
        Text text = (Text)layout.getPageLayout().get(2);
        assertEquals("PageText - & \u00E9 \u00E1 ", text.getText());
        section = (Section) layout.getPageLayout().get(3);
        assertEquals("<html>Number of Persons by<br/> Nationality and Status</html>", section.getLabel());
        text = (Text) layout.getPageLayout().get(4);
        assertEquals("<html><font color='red'><em>DO NOT DELAY</em></font><br/>supply further information</html>", text.getText());

        
        assertNotNull( layout.toXML());
        output = layout.toXML().toString();
        assertEquals(TEST_OUTPUT_SPECIAL, output);
    }

    @Test
    public void testLayoutFromFile() throws XmlPullParserException, IOException, SmackException {
        DataFormProvider pr = new DataFormProvider();
        
        XmlPullParser parser = PacketParserUtils.newXmppParser();
        parser.setInput(new InputStreamReader(this.getClass().getResourceAsStream(TEST_INPUT_1), "UTF-8"));
        parser.next();
        
        DataForm form = pr.parse(parser);
        assertNotNull( form);
        assertEquals(1 , form.getExtensionElements().size());
        
        DataLayout layout = (DataLayout) form.getExtensionElements().get(0);
        
        assertEquals(5 , layout.getPageLayout().size());
        assertEquals("Label - & \u00E9 \u00E1 ", layout.getLabel());
        Section section = (Section) layout.getPageLayout().get(1);
        assertEquals("section Label - & \u00E9 \u00E1 ", section.getLabel());
        Text text = (Text)layout.getPageLayout().get(2);
        assertEquals("PageText - & \u00E9 \u00E1 ", text.getText());
        section = (Section) layout.getPageLayout().get(3);
        assertEquals("<html>Number of Persons by<br/> Nationality and Status</html>", section.getLabel());
        text = (Text) layout.getPageLayout().get(4);
        assertEquals("<html><font color='red'><em>DO NOT DELAY</em></font><br/>supply further information</html>", text.getText());

        
        assertNotNull( layout.toXML());
        String output = layout.toXML().toString();
        assertEquals(TEST_OUTPUT_SPECIAL, output);
    }
}
