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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;

public class UserSearchManager {

    private XMPPConnection con;
    private String serviceName;
    private UserSearch userSearch;

    public UserSearchManager(XMPPConnection con, String serviceName) {
        this.con = con;
        this.serviceName = serviceName;

        userSearch = new UserSearch();
    }

    public Form getSearchForm() {
        try {
            return userSearch.getSearchForm(con, serviceName);
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ReportedData sendSearchForm(Form searchForm) {
        ReportedData data = null;
        try {
            data = userSearch.sendSearchForm(con, searchForm, serviceName);
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }

        return data;
    }

    public static void main(String args[]) throws Exception {
        XMPPConnection.DEBUG_ENABLED = true;
        XMPPConnection con = new XMPPConnection("jabber.org");
        con.login("ddman", "whocares");

        UserSearchManager manager = new UserSearchManager(con, "users.jabber.org");
        Form f = manager.getSearchForm();

        Form answers = f.createAnswerForm();
        answers.setAnswer("first", "Matt");

        ReportedData data = manager.sendSearchForm(answers);
        System.out.println(data);

    }


}
