/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smackx.provider;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

/**
 * The DataFormProvider parses DataForm packets.
 * 
 * @author Gaston Dombiak
 */
public class DataFormProvider implements PacketExtensionProvider {

    /**
     * Creates a new DataFormProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public DataFormProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        boolean done = false;
        StringBuffer buffer = null;
        DataForm dataForm = new DataForm(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("instructions")) { 
                    dataForm.setInstructions(parser.nextText());
                }
                else if (parser.getName().equals("title")) {                    
                    dataForm.setTitle(parser.nextText());
                }
                else if (parser.getName().equals("field")) {                    
                    dataForm.addField(parseField(parser));
                }
                else if (parser.getName().equals("item")) {                    
                    dataForm.addItem(parseItem(parser));
                }
                else if (parser.getName().equals("reported")) {                    
                    dataForm.setReportedData(parseReported(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(dataForm.getElementName())) {
                    done = true;
                }
            }
        }
        return dataForm;
    }

    private FormField parseField(XmlPullParser parser) throws Exception {
        boolean done = false;
        FormField formField = new FormField(parser.getAttributeValue("", "var"));
        formField.setLabel(parser.getAttributeValue("", "label"));
        formField.setType(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("desc")) { 
                    formField.setDescription(parser.nextText());
                }
                else if (parser.getName().equals("value")) {                    
                    formField.addValue(parser.nextText());
                }
                else if (parser.getName().equals("required")) {                    
                    formField.setRequired(true);
                }
                else if (parser.getName().equals("option")) {                    
                    formField.addOption(parseOption(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("field")) {
                    done = true;
                }
            }
        }
        return formField;
    }

    private DataForm.Item parseItem(XmlPullParser parser) throws Exception {
        boolean done = false;
        List fields = new ArrayList();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    done = true;
                }
            }
        }
        return new DataForm.Item(fields);
    }

    private DataForm.ReportedData parseReported(XmlPullParser parser) throws Exception {
        boolean done = false;
        List fields = new ArrayList();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("reported")) {
                    done = true;
                }
            }
        }
        return new DataForm.ReportedData(fields);
    }

    private FormField.Option parseOption(XmlPullParser parser) throws Exception {
        boolean done = false;
        FormField.Option option = null;
        String label = parser.getAttributeValue("", "label");
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("value")) {
                    option = new FormField.Option(label, parser.nextText());                     
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("option")) {
                    done = true;
                }
            }
        }
        return option;
    }
}
