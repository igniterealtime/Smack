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

/**
 * Represents a field of a form. The field could be used to represent a question to complete,
 * a completed question or a data returned from a search. The exact interpretation of the field
 * depends on the context where the field is used.
 * 
 * @author Gaston Dombiak
 */
public class FormField {
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_FIXED = "fixed";
    public static final String TYPE_HIDDEN = "hidden";
    public static final String TYPE_JID_MULTI = "jid-multi";
    public static final String TYPE_JID_SINGLE = "jid-single";
    public static final String TYPE_LIST_MULTI = "list-multi";
    public static final String TYPE_LIST_SINGLE = "list-single";
    public static final String TYPE_TEXT_MULTI = "text-multi";
    public static final String TYPE_TEXT_PRIVATE = "text-private";
    public static final String TYPE_TEXT_SINGLE = "text-single";

    private String description;
    private boolean required = false;
    private String label;
    private String variable;
    private String type;
    private List options = new ArrayList();
    private List values = new ArrayList();

    /**
     * Creates a new FormField with the variable name that uniquely identifies the field 
     * in the context of the form. 
     *  
     * @param variable the variable name of the question.
     */
    public FormField(String variable) {
        this.variable = variable;
    }
    
    /**
     * Returns a description that provides extra clarification about the question. This information
     * could be presented to the user either in tool-tip, help button, or as a section of text 
     * before the question.<p> 
     * 
     * If the question is of type FIXED then the description should remain empty.
     * 
     * @return description that provides extra clarification about the question.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the label of the question which should give enough information to the user to 
     * fill out the form.
     * 
     * @return label of the question.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns an Iterator for the available options that the user has in order to answer
     * the question. 
     * 
     * @return Iterator for the available options. 
     */
    public Iterator getOptions() {
        synchronized (options) {
            return Collections.unmodifiableList(new ArrayList(options)).iterator();
        }
    }

    /**
     * Returns true if the question must be answered in order to complete the questionnaire.
     * 
     * @return true if the question must be answered in order to complete the questionnaire.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Returns an indicative of the format for the data to answer. Valid formats are:
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
     * @return format for the data to answer.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns an Iterator for the default values of the question if the question is part
     * of a form to fill out. Otherwise, returns an Iterator for the answered values of 
     * the question. 
     * 
     * @return an Iterator for the default values or answered values of the question. 
     */
    public Iterator getValues() {
        synchronized (values) {
            return Collections.unmodifiableList(new ArrayList(values)).iterator();
        }
    }

    /**
     * Returns the variable name that the question is filling out.
     * 
     * @return the variable name of the question.
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Sets a description that provides extra clarification about the question. This information
     * could be presented to the user either in tool-tip, help button, or as a section of text 
     * before the question.<p> 
     * 
     * If the question is of type FIXED then the description should remain empty.
     * 
     * @param description provides extra clarification about the question.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the label of the question which should give enough information to the user to 
     * fill out the form.
     * 
     * @param label the label of the question.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets if the question must be answered in order to complete the questionnaire.
     * 
     * @param required if the question must be answered in order to complete the questionnaire.
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Sets an indicative of the format for the data to answer. Valid formats are:
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
     * @param type an indicative of the format for the data to answer.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Adds a default value to the question if the question is part of a form to fill out. 
     * Otherwise, adds an answered value to the question.
     *  
     * @param value a default value or an answered value of the question.
     */
    public void addValue(String value) {
        synchronized (values) {
            values.add(value);
        }
    }

    /**
     * Adds a default values to the question if the question is part of a form to fill out. 
     * Otherwise, adds an answered values to the question.
     *  
     * @param newValues default values or an answered values of the question.
     */
    public void addValues(List newValues) {
        synchronized (values) {
            values.addAll(newValues);
        }
    }

    /**
     * Removes all the values of the field.
     *
     */
    protected void resetValues() {
        synchronized (values) {
            values.removeAll(new ArrayList(values));
        }
    }
    
    /**
     * Adss an available options to the question that the user has in order to answer 
     * the question.
     *  
     * @param option a new available option for the question.
     */
    public void addOption(Option option) {
        synchronized (options) {
            options.add(option);
        }
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<field");
        // Add attributes
        if (getLabel() != null) {
            buf.append(" label=\"").append(getLabel()).append("\"");
        }
        if (getVariable() != null) {
            buf.append(" var=\"").append(getVariable()).append("\"");
        }
        if (getType() != null) {
            buf.append(" type=\"").append(getType()).append("\"");
        }
        buf.append(">");
        // Add elements
        if (getDescription() != null) {
            buf.append("<desc>").append(getDescription()).append("</desc>");
        }
        if (isRequired()) {
            buf.append("<required/>");
        }
        // Loop through all the values and append them to the string buffer
        for (Iterator i = getValues(); i.hasNext();) {
            buf.append("<value>").append(i.next()).append("</value>");
        }
        // Loop through all the values and append them to the string buffer
        for (Iterator i = getOptions(); i.hasNext();) {
            buf.append(((Option)i.next()).toXML());
        }
        buf.append("</field>");
        return buf.toString();
    }

    /**
     * 
     * Represents the available option of a given FormField. 
     * 
     * @author Gaston Dombiak
     */
    public static class Option {
        private String label;
        private String value; 

        public Option(String value) {
            this.value = value;
        }

        public Option(String label, String value) {
            this.label = label;
            this.value = value;
        }
        
        /**
         * Returns the label that represents the option. 
         * 
         * @return the label that represents the option.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the value of the option.
         * 
         * @return the value of the option.
         */
        public String getValue() {
            return value;
        }

        public String toXML() {
            StringBuffer buf = new StringBuffer();
            buf.append("<option");
            // Add attribute
            if (getLabel() != null) {
                buf.append(" label=\"").append(getLabel()).append("\"");
            }
            buf.append(">");
            // Add element
            buf.append("<value>").append(getValue()).append("</value>");

            buf.append("</option>");
            return buf.toString();
        }
    }
}
