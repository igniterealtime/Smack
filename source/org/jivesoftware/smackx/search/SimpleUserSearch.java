/**
 * $RCSfile: ,v $
 * $Revision: $
 * $Date:  $
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */
package org.jivesoftware.smackx.search;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;

import java.util.Iterator;

public class SimpleUserSearch extends IQ {

    private Form form;

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<query xmlns=\"jabber:iq:search\">");
        buf.append(getConvertedPacket());
        buf.append("</query>");
        System.out.println(buf.toString());
        return buf.toString();
    }

    public String getConvertedPacket() {
        StringBuffer buf = new StringBuffer();

        if (form == null) {
            form = Form.getFormFrom(this);
        }
        Iterator fields = form.getFields();
        while (fields.hasNext()) {
            FormField field = (FormField) fields.next();
            String name = field.getVariable();
            String value = getValue(field);
            if (value.trim().length() > 0) {
                buf.append("<").append(name).append(">").append(value).append("</").append(name).append(">");
            }
        }

        return buf.toString();
    }

    public String getValue(FormField formField) {
        Iterator values = formField.getValues();
        while (values.hasNext()) {
            return (String) values.next();
        }
        return "";
    }


}
