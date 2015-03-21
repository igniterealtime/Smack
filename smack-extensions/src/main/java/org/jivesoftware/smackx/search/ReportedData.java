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
package org.jivesoftware.smackx.search;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.packet.DataForm.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of data results returned as part of a search. The report is structured 
 * in columns and rows.
 * 
 * @author Gaston Dombiak
 */
public class ReportedData {
    
    private List<Column> columns = new ArrayList<Column>();
    private List<Row> rows = new ArrayList<Row>();
    private String title = "";
    
    /**
     * Returns a new ReportedData if the stanza(/packet) is used for reporting data and includes an 
     * extension that matches the elementName and namespace "x","jabber:x:data".
     * 
     * @param packet the stanza(/packet) used for reporting data.
     */
    public static ReportedData getReportedDataFrom(Stanza packet) {
        // Check if the packet includes the DataForm extension
        DataForm dataForm = DataForm.from(packet);
        if (dataForm != null) {
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
        for (FormField field : dataForm.getReportedData().getFields()) {
            columns.add(new Column(field.getLabel(), field.getVariable(), field.getType()));
        }

        // Add the rows to the report based on the form's items
        for (Item item : dataForm.getItems()) {
            List<Field> fieldList = new ArrayList<Field>(columns.size());
            for (FormField field : item.getFields()) {
                // The field is created with all the values of the data form's field
                List<String> values = new ArrayList<String>();
                for (String value : field.getValues()) {
                    values.add(value);
                }
                fieldList.add(new Field(field.getVariable(), values));
            }
            rows.add(new Row(fieldList));
        }

        // Set the report's title
        this.title = dataForm.getTitle();
    }


    public ReportedData(){
        // Allow for model creation of ReportedData.
    }

    /**
     * Adds a new <code>Row</code>.
     * @param row the new row to add.
     */
    public void addRow(Row row){
        rows.add(row);
    }

    /**
     * Adds a new <code>Column</code>
     * @param column the column to add.
     */
    public void addColumn(Column column){
        columns.add(column);
    }


    /**
     * Returns a List of the rows returned from a search.
     *
     * @return a List of the rows returned from a search.
     */
    public List<Row> getRows() {
        return Collections.unmodifiableList(new ArrayList<Row>(rows));
    }

    /**
     * Returns a List of the columns returned from a search.
     *
     * @return a List of the columns returned from a search.
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(new ArrayList<Column>(columns));
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
        private final String label;
        private final String variable;
        private final FormField.Type type;

        /**
         * Creates a new column with the specified definition.
         *
         * @param label the columns's label.
         * @param variable the variable name of the column.
         * @param type the format for the returned data.
         */
        public Column(String label, String variable, FormField.Type type) {
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
         * Returns the column's data format.
         *
         * @return format for the returned data.
         */
        public FormField.Type getType() {
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
        private List<Field> fields = new ArrayList<Field>();

        public Row(List<Field> fields) {
            this.fields = fields;
        }

        /**
         * Returns the values of the field whose variable matches the requested variable.
         *
         * @param variable the variable to match.
         * @return the values of the field whose variable matches the requested variable.
         */
        public List<String> getValues(String variable) {
            for(Field field : getFields()) {
                if (variable.equalsIgnoreCase(field.getVariable())) {
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
        private List<Field> getFields() {
            return Collections.unmodifiableList(new ArrayList<Field>(fields));
        }
    }

    public static class Field {
        private String variable;
        private List<String> values;

        public Field(String variable, List<String> values) {
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
         * Returns a List of the values reported as part of the search.
         * 
         * @return the returned values of the search.
         */
        public List<String> getValues() {
            return Collections.unmodifiableList(values);
        }
    }
}
