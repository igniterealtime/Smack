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

package org.jivesoftware.smackx;

import java.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.DataForm;

/**
 * Represents a set of data results returned as part of a search. The report is structured 
 * in columns and rows.
 * 
 * @author Gaston Dombiak
 */
public class ReportedData {
    
    private List columns = new ArrayList();
    private List rows = new ArrayList();
    private String title = "";
    
    /**
     * Returns a new ReportedData if the packet is used for reporting data and includes an 
     * extension that matches the elementName and namespace "x","jabber:x:data".
     * 
     * @param packet the packet used for reporting data.
     */
    public static ReportedData getReportedDataFrom(Packet packet) {
        // Check if the packet includes the DataForm extension
        PacketExtension packetExtension = packet.getExtension("x","jabber:x:data");
        if (packetExtension != null) {
            // Check if the existing DataForm is a result of a search
            DataForm dataForm = (DataForm) packetExtension;
            if (dataForm.getReportedData() != null)
                return new ReportedData(dataForm);
        }
        // Otherwise return null
        return null;
    }
    
    
    /**
     * Creates a new ReportedData based on the returned dataForm from a search
     *(namespace "jabber:iq:search"). 
     * 
     * @param dataForm the dataForm returned from a search (namespace "jabber:iq:search"). 
     */
    private ReportedData(DataForm dataForm) {
        // Add the columns to the report based on the reported data fields
        for (Iterator fields = dataForm.getReportedData().getFields(); fields.hasNext();) {
            FormField field = (FormField)fields.next();
            columns.add(new Column(field.getLabel(), field.getVariable(), field.getType()));
        }

        // Add the rows to the report based on the form's items
        for (Iterator items = dataForm.getItems(); items.hasNext();) {
            DataForm.Item item = (DataForm.Item)items.next();
            List fieldList = new ArrayList(columns.size());
            FormField field;
            for (Iterator fields = item.getFields(); fields.hasNext();) {
                field = (FormField) fields.next();
                // Note: The field is created based on the FIRST value of the data form's field  
                fieldList.add(new Field(field.getVariable(), (String)field.getValues().next()));
            }
            rows.add(new Row(fieldList));
        }

        // Set the report's title
        this.title = dataForm.getTitle();
    }
    
    /**
     * Returns an Iterator for the rows returned from a search.
     *
     * @return an Iterator for the rows returned from a search.
     */
    public Iterator getRows() {
        return Collections.unmodifiableList(new ArrayList(rows)).iterator();
    }

    /**
     * Returns an Iterator for the columns returned from a search.
     * 
     * @return an Iterator for the columns returned from a search.
     */
    public Iterator getColumns() {
        return Collections.unmodifiableList(new ArrayList(columns)).iterator();
    }


    /**
     * Returns the report's title. It is similar to the title on a web page or an X 
     * window.
     * 
     * @return title of the report.
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * 
     * Represents the columns definition of the reported data.
     * 
     * @author Gaston Dombiak
     */
    public static class Column {
        private String label;
        private String variable;
        private String type;

        /**
         * Creates a new column with the specified definition.
         * 
         * @param label the columns's label.
         * @param variable the variable name of the column.
         * @param type the format for the returned data.
         */
        private Column(String label, String variable, String type) {
            this.label = label;
            this.variable = variable;
            this.type = type;
        }
        
        /**
         * Returns the column's label.
         * 
         * @return label of the column.
         */
        public String getLabel() {
            return label;
        }


        /**
         * Returns the column's data format. Valid formats are:
         * 
         * <ul>
         *  <li>text-single -> single line or word of text
         *  <li>text-private -> instead of showing the user what they typed, you show ***** to 
         * protect it
         *  <li>text-multi -> multiple lines of text entry
         *  <li>list-single -> given a list of choices, pick one
         *  <li>list-multi -> given a list of choices, pick one or more
         *  <li>boolean -> 0 or 1, true or false, yes or no. Default value is 0
         *  <li>fixed -> fixed for putting in text to show sections, or just advertise your web 
         * site in the middle of the form
         *  <li>hidden -> is not given to the user at all, but returned with the questionnaire
         *  <li>jid-single -> Jabber ID - choosing a JID from your roster, and entering one based 
         * on the rules for a JID.
         *  <li>jid-multi -> multiple entries for JIDs
         * </ul>
         * 
         * @return format for the returned data.
         */
        public String getType() {
            return type;
        }


        /**
         * Returns the variable name that the column is showing.
         * 
         * @return the variable name of the column.
         */
        public String getVariable() {
            return variable;
        }


    }
    
    public static class Row {
        private List fields = new ArrayList();
        
        private Row(List fields) {
            this.fields = fields;
        }
        
        /**
         * Returns the value of the field whose variable matches the requested variable.
         * 
         * @param variable the variable to match.
         * @return the value of the field whose variable matches the requested variable.
         */
        public String getValue(String variable) {
            for(Iterator it=getFields();it.hasNext();) {
                Field field = (Field) it.next();
                if (variable.equals(field.getVariable())) {
                    return field.getValue();
                }
            }
            return null;
        }
        
        /**
         * Returns the fields that define the data that goes with the item.
         * 
         * @return the fields that define the data that goes with the item.
         */
        private Iterator getFields() {
            return Collections.unmodifiableList(new ArrayList(fields)).iterator();
        }
    }
    
    private static class Field {
        private String variable;
        private String value;

        private Field(String variable, String value) {
            this.variable = variable;
            this.value = value;
        }

        /**
         * Returns the variable name that the field represents.
         * 
         * @return the variable name of the field.
         */
        public String getVariable() {
            return variable;
        }

        /**
         * Returns the value reported as part of the search.
         * 
         * @return the returned value of the search.
         */
        public String getValue() {
            return value;
        }
    }
}
