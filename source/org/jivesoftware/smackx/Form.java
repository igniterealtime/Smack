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
 * Represents a Form for gathering data. The form could be of the following types:
 * <ul>
 *  <li>form -> Indicates a form to fill out.</li>
 *  <li>submit -> The form is filled out, and this is the data that is being returned from 
 * the form.</li>
 *  <li>cancel -> The form was cancelled. Tell the asker that piece of information.</li>
 *  <li>result -> Data results being returned from a search, or some other query.</li>
 * </ul>
 * 
 * Depending of the form's type different operations are available. For example, if the form
 * is of type "submit", it's not possible to add a new Field to the form.
 * 
 * @author Gaston Dombiak
 */
public class Form {
    
    public static final String TYPE_FORM = "form";
    public static final String TYPE_SUBMIT = "submit";
    public static final String TYPE_RESULT = "result";
    
    private DataForm dataForm;
    
    /**
     * Returns a new ReportedData if the packet is used for gathering data and includes an 
     * extension that matches the elementName and namespace "x","jabber:x:data".  
     * 
     * @param packet the packet used for gathering data.
     */
    public static Form getFormFrom(Packet packet) {
        // Check if the packet includes the DataForm extension
        PacketExtension packetExtension = packet.getExtension("x","jabber:x:data");
        if (packetExtension != null) {
            // Check if the existing DataForm is not a result of a search
            DataForm dataForm = (DataForm) packetExtension;
            if (dataForm.getReportedData() == null)
                return new Form(dataForm);
        }
        // Otherwise return null
        return null;
    }

    /**
     * Creates a new Form that will wrap an existing DataForm. The wrapped DataForm must be
     * used for gathering data. 
     * 
     * @param dataForm the data form used for gathering data. 
     */
    private Form(DataForm dataForm) {
        this.dataForm = dataForm;
    }
    
    /**
     * Creates a new Form of a given type from scratch.<p>
     *  
     * Possible form types are:
     * <ul>
     *  <li>form -> Indicates a form to fill out.</li>
     *  <li>submit -> The form is filled out, and this is the data that is being returned from 
     * the form.</li>
     *  <li>cancel -> The form was cancelled. Tell the asker that piece of information.</li>
     *  <li>result -> Data results being returned from a search, or some other query.</li>
     * </ul>
     * 
     * @param type the form's type (e.g. form, submit,cancel,result).
     */
    public Form(String type) {
        this.dataForm = new DataForm(type);
    }
    
    /**
     * Adds a new field to complete as part of the form.
     * 
     * @param field the field to complete.
     * @throws IllegalStateException if the form is not of type "form".
     */
    public void addField(FormField field) {
        if (!isFormType()) {
            throw new IllegalStateException("Cannot add fields if the form is not of type \"form\"");
        }
        dataForm.addField(field);
    }
    
    /**
     * Adds a new answer as part of the form. The answer to add is a FormField whose variable
     * and value attributes are completed.
     * 
     * @param variable the variable that was completed.
     * @param value the value that was answered.
     * @throws IllegalStateException if the form is not of type "submit".
     */
    public void addAnswer(String variable, String value) {
        if (!isSubmitType()) {
            throw new IllegalStateException("Cannot add fields if the form is not of type \"submit\"");
        }
        FormField field = new FormField(variable);
        field.addValue(value);
        dataForm.addField(field);
    }

    /**
     * Adds a new answer as part of the form. The answer to add is a FormField whose variable
     * and values attributes are completed.
     * 
     * @param variable the variable that was completed.
     * @param values the values that were answered.
     * @throws IllegalStateException if the form is not of type "submit".
     */
    public void addAnswer(String variable, List values) {
        if (!isSubmitType()) {
            throw new IllegalStateException("Cannot add fields if the form is not of type \"submit\"");
        }
        FormField field = new FormField(variable);
        field.addValues(values);
        dataForm.addField(field);
    }

    /**
     * Returns an Iterator for the fields that are part of the form.
     *
     * @return an Iterator for the fields that are part of the form.
     */
    public Iterator getFields() {
        return dataForm.getFields();
    }


    /**
     * Returns the instructions that explain how to fill out the form and what the form is about.
     * 
     * @return instructions that explain how to fill out the form.
     */
    public String getInstructions() {
        return dataForm.getInstructions();
    }


    /**
     * Returns the description of the data. It is similar to the title on a web page or an X 
     * window.  You can put a <title/> on either a form to fill out, or a set of data results.
     * 
     * @return description of the data.
     */
    public String getTitle() {
        return dataForm.getTitle();
    }


    /**
     * Returns the meaning of the data within the context. The data could be part of a form
     * to fill out, a form submission or data results.<p>
     * 
     * Possible form types are:
     * <ul>
     *  <li>form -> Indicates a form to fill out.</li>
     *  <li>submit -> The form is filled out, and this is the data that is being returned from 
     * the form.</li>
     *  <li>cancel -> The form was cancelled. Tell the asker that piece of information.</li>
     *  <li>result -> Data results being returned from a search, or some other query.</li>
     * </ul>
     * 
     * @return the form's type.
     */
    public String getType() {
        return dataForm.getType(); 
    }
    

    /**
     * Sets instructions that explain how to fill out the form and what the form is about.
     * 
     * @param instructions instructions that explain how to fill out the form.
     */
    public void setInstructions(String instructions) {
        dataForm.setInstructions(instructions);
    }


    /**
     * Sets the description of the data. It is similar to the title on a web page or an X window.
     * You can put a <title/> on either a form to fill out, or a set of data results.
     * 
     * @param title description of the data.
     */
    public void setTitle(String title) {
        dataForm.setTitle(title);
    }
    
    /**
     * Returns the wrapped DataForm.
     * 
     * @return the wrapped DataForm.
     */
    DataForm getDataForm() {
        return dataForm;
    }
    
    /**
     * Returns true if the form is a form to fill out.
     * 
     * @return if the form is a form to fill out.
     */
    private boolean isFormType() {
        return TYPE_FORM.equals(dataForm.getType());
    }
    
    /**
     * Returns true if the form is a form to submit.
     * 
     * @return if the form is a form to submit.
     */
    private boolean isSubmitType() {
        return TYPE_SUBMIT.equals(dataForm.getType());
    }

    /**
     * Returns a new Form to submit the completed values. The new Form will include the hidden
     * fields of the original form.
     * 
     * @return a Form to submit the completed values.
     */
    public Form createAnswerForm() {
        if (!isFormType()) {
            throw new IllegalStateException("Only forms of type \"form\" could be answered");
        }
        // Create a new Form
        Form form = new Form(TYPE_SUBMIT);
        // Copy the hidden fields to the new form
        for (Iterator fields=getFields(); fields.hasNext();) {
            FormField field = (FormField)fields.next();
            if (FormField.TYPE_HIDDEN.equals(field.getType())) {
                // Since a hidden field could have many values we need to collect them in a list
                List values = new ArrayList();
                for (Iterator it=field.getValues();it.hasNext();) {
                    values.add((String)it.next());
                }
                form.addAnswer(field.getVariable(), values);
            }
        }
        return form;
    }

}
