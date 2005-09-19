/**
 * $RCSfile: ,v $
 * $Revision: 1.0 $
 * $Date: 2005/05/25 04:20:03 $
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is
 subject to license terms.
 */

package org.jivesoftware.smackx.search;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;

public class Test {

    public static void main(String args[]) throws Exception {
        XMPPConnection.DEBUG_ENABLED = true;
        XMPPConnection con = new XMPPConnection("jabber.org");
        con.login("ddman", "whocares");

        UserSearchManager search = new UserSearchManager(con, "users.jabber.org");
        Form searchForm = search.getSearchForm();

        Form answerForm = searchForm.createAnswerForm();
        answerForm.setAnswer("last", "Lynch");

        ReportedData data = search.getSearchResults(answerForm);


        System.out.println(data);

    }
}
