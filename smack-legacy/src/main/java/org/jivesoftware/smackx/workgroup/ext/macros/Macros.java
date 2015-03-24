/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.ext.macros;

import java.io.IOException;
import java.io.StringReader;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;

/**
 * Macros iq is responsible for handling global and personal macros in the a Live Assistant
 * Workgroup.
 */
public class Macros extends IQ {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "macros";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private MacroGroup rootGroup;
    private boolean personal;
    private MacroGroup personalMacroGroup;

    public Macros() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public MacroGroup getRootGroup() {
        return rootGroup;
    }

    public void setRootGroup(MacroGroup rootGroup) {
        this.rootGroup = rootGroup;
    }

    public boolean isPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public MacroGroup getPersonalMacroGroup() {
        return personalMacroGroup;
    }

    public void setPersonalMacroGroup(MacroGroup personalMacroGroup) {
        this.personalMacroGroup = personalMacroGroup;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();

        if (isPersonal()) {
            buf.append("<personal>true</personal>");
        }
        if (getPersonalMacroGroup() != null) {
            // CHECKSTYLE:OFF
        	buf.append("<personalMacro>");
        	buf.append(StringUtils.escapeForXML(getPersonalMacroGroup().toXML()));
        	buf.append("</personalMacro>");
            // CHECKSTYLE:ON
        }

        return buf;
    }

    /**
     * An IQProvider for Macro packets.
     *
     * @author Derek DeMoro
     */
    public static class InternalProvider extends IQProvider<Macros> {

        @Override
        public Macros parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
            Macros macroGroup = new Macros();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("model")) {
                        String macros = parser.nextText();
                        MacroGroup group = parseMacroGroups(macros);
                        macroGroup.setRootGroup(group);
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(ELEMENT_NAME)) {
                        done = true;
                    }
                }
            }

            return macroGroup;
        }

        public Macro parseMacro(XmlPullParser parser) throws XmlPullParserException, IOException {
            // CHECKSTYLE:OFF
        	Macro macro = new Macro();
        	 boolean done = false;
            while (!done) {
	        	int eventType = parser.next();
	        	if (eventType == XmlPullParser.START_TAG) {
	        		if (parser.getName().equals("title")) {
	        			parser.next();
	        			macro.setTitle(parser.getText());
	        		}
	        		else if (parser.getName().equals("description")) {
	        			macro.setDescription(parser.nextText());
	        		}
	        		else if (parser.getName().equals("response")) {
	        			macro.setResponse(parser.nextText());
	        		}
	        		else if (parser.getName().equals("type")) {
	        			macro.setType(Integer.valueOf(parser.nextText()).intValue());
	        		}
	        	}
	            else if (eventType == XmlPullParser.END_TAG) {
	                if (parser.getName().equals("macro")) {
	                    done = true;
	                }
	            }
            }
        	return macro;
            // CHECKSTYLE:ON
        }

        public MacroGroup parseMacroGroup(XmlPullParser parser) throws XmlPullParserException, IOException {
            // CHECKSTYLE:OFF
        	MacroGroup group = new MacroGroup();

            boolean done = false;
            while (!done) {
	        	int eventType = parser.next();
	        	if (eventType == XmlPullParser.START_TAG) {
	        		if (parser.getName().equals("macrogroup")) {
	        			group.addMacroGroup(parseMacroGroup(parser));
	        		}
	        		if (parser.getName().equals("title")) {
	        			group.setTitle(parser.nextText());
	        		}
	        		if (parser.getName().equals("macro")) {
	        			group.addMacro(parseMacro(parser));
	        		}
	        	}
	            else if (eventType == XmlPullParser.END_TAG) {
	                if (parser.getName().equals("macrogroup")) {
	                    done = true;
	                }
	            }
            }
        	return group; 
            // CHECKSTYLE:ON
        }

        public MacroGroup parseMacroGroups(String macros) throws XmlPullParserException, IOException {
            // CHECKSTYLE:OFF
        	MacroGroup group = null;
        	XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        	parser.setInput(new StringReader(macros));
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {		
				eventType = parser.next();
				 if (eventType == XmlPullParser.START_TAG) {
	                    if (parser.getName().equals("macrogroup")) {
	                    	group = parseMacroGroup(parser);
	                    }
				 }
			}
			return group;
            // CHECKSTYLE:ON
        }
    }
}
