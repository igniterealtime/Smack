/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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
                // The field is created with all the values of the data form's field
                List values = new ArrayList();
                for (Iterator it=field.getValues(); it.hasNext();) {
                    values.add(it.next());
                }
                fieldList.add(new Field(field.getVariable(), values));
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
         * Returns the values of the field whose variable matches the requested variable.
         * 
         * @param variable the variable to match.
         * @return the values of the field whose variable matches the requested variable.
         */
        public Iterator getValues(String variable) {
            for(Iterator it=getFields();it.hasNext();) {
                Field field = (Field) it.next();
                if (variable.equals(field.getVariable())) {
                    return field.getValues();
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
        private List values;

        private Field(String variable, List values) {
            this.variable = variable;
            this.values = values;
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
         * Returns an iterator on the values reported as part of the search.
         * 
         * @return the returned values of the search.
         */
        public Iterator getValues() {
            return Collections.unmodifiableList(values).iterator();
        }
    }
}
