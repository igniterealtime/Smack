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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.FormField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a form that could be use for gathering data as well as for reporting data
 * returned from a search.
 *
 * @author Gaston Dombiak
 */
public class DataForm implements PacketExtension {

    private String type;
    private String title;
    private List<String> instructions = new ArrayList<String>();
    private ReportedData reportedData;
    private final List<Item> items = new ArrayList<Item>();
    private final List<FormField> fields = new ArrayList<FormField>();
    
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
     * Returns an Iterator for the list of instructions that explain how to fill out the form and 
     * what the form is about. The dataform could include multiple instructions since each 
     * instruction could not contain newlines characters. Join the instructions together in order 
     * to show them to the user.    
     * 
     * @return an Iterator for the list of instructions that explain how to fill out the form.
     */
    public Iterator<String> getInstructions() {
        synchronized (instructions) {
            return Collections.unmodifiableList(new ArrayList<String>(instructions)).iterator();
        }
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
    public Iterator<Item> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList<Item>(items)).iterator();
        }
    }

    /**
     * Returns an Iterator for the fields that are part of the form.
     *
     * @return an Iterator for the fields that are part of the form.
     */
    public Iterator<FormField> getFields() {
        synchronized (fields) {
            return Collections.unmodifiableList(new ArrayList<FormField>(fields)).iterator();
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
     * Sets the list of instructions that explain how to fill out the form and what the form is 
     * about. The dataform could include multiple instructions since each instruction could not 
     * contain newlines characters. 
     * 
     * @param instructions list of instructions that explain how to fill out the form.
     */
    public void setInstructions(List<String> instructions) {
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
     * Adds a new instruction to the list of instructions that explain how to fill out the form 
     * and what the form is about. The dataform could include multiple instructions since each 
     * instruction could not contain newlines characters. 
     * 
     * @param instruction the new instruction that explain how to fill out the form.
     */
    public void addInstruction(String instruction) {
        synchronized (instructions) {
            instructions.add(instruction);
        }
    }

    /**
     * Adds a new item returned from a search.
     * 
     * @param item the item returned from a search.
     */
    public void addItem(Item item) {
        synchronized (items) {
            items.add(item);
        }
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\" type=\"" + getType() +"\">");
        if (getTitle() != null) {
            buf.append("<title>").append(getTitle()).append("</title>");
        }
        for (Iterator it=getInstructions(); it.hasNext();) {
            buf.append("<instructions>").append(it.next()).append("</instructions>");
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
        private List<FormField> fields = new ArrayList<FormField>();
        
        public ReportedData(List<FormField> fields) {
            this.fields = fields;
        }
        
        /**
         * Returns the fields returned from a search.
         * 
         * @return the fields returned from a search.
         */
        public Iterator<FormField> getFields() {
            return Collections.unmodifiableList(new ArrayList<FormField>(fields)).iterator();
        }
        
        public String toXML() {
            StringBuilder buf = new StringBuilder();
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
        private List<FormField> fields = new ArrayList<FormField>();
        
        public Item(List<FormField> fields) {
            this.fields = fields;
        }
        
        /**
         * Returns the fields that define the data that goes with the item.
         * 
         * @return the fields that define the data that goes with the item.
         */
        public Iterator<FormField> getFields() {
            return Collections.unmodifiableList(new ArrayList<FormField>(fields)).iterator();
        }
        
        public String toXML() {
            StringBuilder buf = new StringBuilder();
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
