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
package org.jivesoftware.smackx.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.FormField;

/**
 * Represents a form that could be use for gathering data as well as for reporting data
 * returned from a search.
 *
 * @author Gaston Dombiak
 */
public class DataForm implements PacketExtension {

    private String type;
    private String title;
    private String instructions;
    private ReportedData reportedData;
    private List items = new ArrayList();
    private List fields = new ArrayList();
    
    public DataForm(String type) {
        this.type = type;
    }
    
    /**
     * Returns the meaning of the data within the context. The data could be part of a form
     * to fill out, a form submission or data results.<p>
     * 
     * Possible form types are:
     * <ul>
     *  <li>form -> This packet contains a form to fill out. Display it to the user (if your 
     * program can).</li>
     *  <li>submit -> The form is filled out, and this is the data that is being returned from 
     * the form.</li>
     *  <li>cancel -> The form was cancelled. Tell the asker that piece of information.</li>
     *  <li>result -> Data results being returned from a search, or some other query.</li>
     * </ul>
     * 
     * @return the form's type.
     */
    public String getType() {
        return type; 
    }
    
    /**
     * Returns the description of the data. It is similar to the title on a web page or an X 
     * window.  You can put a <title/> on either a form to fill out, or a set of data results.
     * 
     * @return description of the data.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the instructions that explain how to fill out the form and what the form is about.
     * 
     * @return instructions that explain how to fill out the form.
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * Returns the fields that will be returned from a search.
     * 
     * @return fields that will be returned from a search.
     */
    public ReportedData getReportedData() {
        return reportedData;
    }

    /**
     * Returns an Iterator for the items returned from a search.
     *
     * @return an Iterator for the items returned from a search.
     */
    public Iterator getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList(items)).iterator();
        }
    }

    /**
     * Returns an Iterator for the fields that are part of the form.
     *
     * @return an Iterator for the fields that are part of the form.
     */
    public Iterator getFields() {
        synchronized (fields) {
            return Collections.unmodifiableList(new ArrayList(fields)).iterator();
        }
    }

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "jabber:x:data";
    }

    /**
     * Sets the description of the data. It is similar to the title on a web page or an X window.
     * You can put a <title/> on either a form to fill out, or a set of data results.
     * 
     * @param title description of the data.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets instructions that explain how to fill out the form and what the form is about.
     * 
     * @param instructions instructions that explain how to fill out the form.
     */
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    /**
     * Sets the fields that will be returned from a search.
     * 
     * @param reportedData the fields that will be returned from a search.
     */
    public void setReportedData(ReportedData reportedData) {
        this.reportedData = reportedData;
    }

    /**
     * Adds a new field as part of the form.
     * 
     * @param field the field to add to the form.
     */
    public void addField(FormField field) {
        synchronized (fields) {
            fields.add(field);
        }
    }
    
    /**
     * Adds a new item returned from a search.
     * 
     * @param field the item returned from a search.
     */
    public void addItem(Item item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\" type=\"" + getType() +"\">");
        if (getTitle() != null) {
            buf.append("<title>").append(getTitle()).append("</title>");
        }
        if (getInstructions() != null) {
            buf.append("<instructions>").append(getInstructions()).append("</instructions>");
        }
        // Append the list of fields returned from a search
        if (getReportedData() != null) {
            buf.append(getReportedData().toXML());
        }
        // Loop through all the items returned from a search and append them to the string buffer
        for (Iterator i = getItems(); i.hasNext();) {
            Item item = (Item) i.next();
            buf.append(item.toXML());
        }
        // Loop through all the form fields and append them to the string buffer
        for (Iterator i = getFields(); i.hasNext();) {
            FormField field = (FormField) i.next();
            buf.append(field.toXML());
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * 
     * Represents the fields that will be returned from a search. This information is useful when 
     * you try to use the jabber:iq:search namespace to return dynamic form information.
     *
     * @author Gaston Dombiak
     */
    public static class ReportedData {
        private List fields = new ArrayList();
        
        public ReportedData(List fields) {
            this.fields = fields;
        }
        
        /**
         * Returns the fields returned from a search.
         * 
         * @return the fields returned from a search.
         */
        public Iterator getFields() {
            return Collections.unmodifiableList(new ArrayList(fields)).iterator();
        }
        
        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<reported>");
            // Loop through all the form items and append them to the string buffer
            for (Iterator i = getFields(); i.hasNext();) {
                FormField field = (FormField) i.next();
                buf.append(field.toXML());
            }
            buf.append("</reported>");
            return buf.toString();
        }
    }
    
    /**
     * 
     * Represents items of reported data.
     *
     * @author Gaston Dombiak
     */
    public static class Item {
        private List fields = new ArrayList();
        
        public Item(List fields) {
            this.fields = fields;
        }
        
        /**
         * Returns the fields that define the data that goes with the item.
         * 
         * @return the fields that define the data that goes with the item.
         */
        public Iterator getFields() {
            return Collections.unmodifiableList(new ArrayList(fields)).iterator();
        }
        
        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<item>");
            // Loop through all the form items and append them to the string buffer
            for (Iterator i = getFields(); i.hasNext();) {
                FormField field = (FormField) i.next();
                buf.append(field.toXML());
            }
            buf.append("</item>");
            return buf.toString();
        }
    }
}
