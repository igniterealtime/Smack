/**
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

package org.jivesoftware.smackx.search;

import org.jivesoftware.smack.Connection;
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
 * Connection con = new XMPPConnection("jabber.org");
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

    private Connection con;
    private UserSearch userSearch;

    /**
     * Creates a new UserSearchManager.
     *
     * @param con the Connection to use.
     */
    public UserSearchManager(Connection con) {
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
