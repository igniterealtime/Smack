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
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The UserSearchManager is a facade built upon Jabber Search Services (JEP-055) to allow for searching
 * repositories on a Jabber Server. This implementation allows for transparency of implementation of
 * searching (DataForms or No DataForms), but allows the user to simply use the DataForm model for both
 * types of support.
 * <pre>
 * XMPPConnection con = new XMPPConnection("jabber.org");
 * con.login("john", "doe");
 * UserSearchManager search = new UserSearchManager(con, "users.jabber.org");
 * Form searchForm = search.getSearchForm();
 * Form answerForm = searchForm.createAnswerForm();
 * answerForm.setAnswer("last", "DeMoro");
 * ReportedData data = search.getSearchResults(answerForm);
 * // Use Returned Data
 * </pre>
 *
 * @author Derek DeMoro
 */
public class UserSearchManager {

    private XMPPConnection con;
    private UserSearch userSearch;

    /**
     * Creates a new UserSearchManager.
     *
     * @param con the XMPPConnection to use.
     */
    public UserSearchManager(XMPPConnection con) {
        this.con = con;
        userSearch = new UserSearch();
    }

    /**
     * Returns the form to fill out to perform a search.
     *
     * @param searchService the search service to query.
     * @return the form to fill out to perform a search.
     * @throws XMPPException thrown if a server error has occurred.
     */
    public Form getSearchForm(String searchService) throws XMPPException {
        return userSearch.getSearchForm(con, searchService);
    }

    /**
     * Submits a search form to the server and returns the resulting information
     * in the form of <code>ReportedData</code>
     *
     * @param searchForm    the <code>Form</code> to submit for searching.
     * @param searchService the name of the search service to use.
     * @return the ReportedData returned by the server.
     * @throws XMPPException thrown if a server error has occurred.
     */
    public ReportedData getSearchResults(Form searchForm, String searchService) throws XMPPException {
        return userSearch.sendSearchForm(con, searchForm, searchService);
    }


    /**
     * Returns a collection of search services found on the server.
     *
     * @return a Collection of search services found on the server.
     * @throws XMPPException thrown if a server error has occurred.
     */
    public Collection getSearchServices() throws XMPPException {
        final List<String> searchServices = new ArrayList<String>();
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(con);
        DiscoverItems items = discoManager.discoverItems(con.getServiceName());
        Iterator<DiscoverItems.Item> iter = items.getItems();
        while (iter.hasNext()) {
            DiscoverItems.Item item = iter.next();
            try {
                DiscoverInfo info;
                try {
                    info = discoManager.discoverInfo(item.getEntityID());
                }
                catch (XMPPException e) {
                    // Ignore Case
                    continue;
                }

                if (info.containsFeature("jabber:iq:search")) {
                    searchServices.add(item.getEntityID());
                }
            }
            catch (Exception e) {
                // No info found.
                break;
            }
        }
        return searchServices;
    }
}
