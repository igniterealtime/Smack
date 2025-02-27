/**
 *
 * Copyright 2003-2007 Jive Software, 2025 Florian Schmaus.
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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.DomainBareJid;

/**
 * The UserSearchManager is a facade built upon Jabber Search Services (XEP-055) to allow for searching
 * repositories on a Jabber Server. This implementation allows for transparency of implementation of
 * searching (DataForms or No DataForms), but allows the user to simply use the DataForm model for both
 * types of support.
 * <pre>
 * XMPPConnection connection = …;
 * var searchService = UserSearchManager.getSearchServices(connection).get(0);
 * var searchManager = UserSearchManager.getInstanceFor(connection);
 * var sarchForm = searchManager.getSearchForm(searchService);
 * var fillableForm = searchForm.getFillableForm();
 *
 * // Check for the required fields in the form and fill them
 * fillableForm.setAnswer("search", "John");
 *
 * var results = searchOne.search(fillableForm, userSearchService);
 * // Use results
 * </pre>
 *
 * @author Derek DeMoro
 */
public final class UserSearchManager extends Manager {

    private static final Map<XMPPConnection, UserSearchManager> INSTANCES = new WeakHashMap<>();

    public static synchronized UserSearchManager getInstanceFor(XMPPConnection connection) {
        var userSearchManager = INSTANCES.get(connection);
        if (userSearchManager == null) {
            userSearchManager = new UserSearchManager(connection);
            INSTANCES.put(connection, userSearchManager);
        }
        return userSearchManager;
    }
    /**
     * Creates a new UserSearchManager.
     *
     * @param connection the XMPPConnection to use.
     */
    private UserSearchManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * Returns the form to fill out to perform a search.
     *
     * @param searchService the search service to query.
     * @return the form to fill out to perform a search.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Form getSearchForm(DomainBareJid searchService) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.get);
        search.setTo(searchService);

        IQ response = connection().sendIqRequestAndWaitForResponse(search);
        var dataForm = DataForm.from(response, UserSearch.NAMESPACE);
        return new Form(dataForm);
    }

    /**
     * Sends the filled out answer form to be sent and queried by the search service.
     *
     * @param filledForm    the filled form with the query instructions.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return ReportedData the data found from the query.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ReportedData search(FillableForm filledForm, DomainBareJid searchService)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.set);
        search.setTo(searchService);
        search.addExtension(filledForm.getDataFormToSubmit());

        IQ response = connection().sendIqRequestAndWaitForResponse(search);
        return ReportedData.getReportedDataFrom(response);
    }

    /**
     * Sends the filled out answer form to be sent and queried by the search service.
     *
     * @param searchForm    the <code>Form</code> to send for querying.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return ReportedData the data found from the query.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ReportedData sendSimpleSearchForm(DataForm searchForm, DomainBareJid searchService) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        SimpleUserSearch search = new SimpleUserSearch();
        search.setForm(searchForm);
        search.setType(IQ.Type.set);
        search.setTo(searchService);

        SimpleUserSearch response = connection().sendIqRequestAndWaitForResponse(search);
        return response.getReportedData();
    }

    /**
     * Returns a collection of search services found on the server.
     *
     * @param connection the connection to query for search services.
     * @return a Collection of search services found on the server.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public static List<DomainBareJid> getSearchServices(XMPPConnection connection) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
        return discoManager.findServices(UserSearch.NAMESPACE, false, true);
    }
}
