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

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.SimpleIQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.xmlpull.v1.XmlPullParser;

/**
 * Implements the protocol currently used to search information repositories on the Jabber network. To date, the jabber:iq:search protocol
 * has been used mainly to search for people who have registered with user directories (e.g., the "Jabber User Directory" hosted at users.jabber.org).
 * However, the jabber:iq:search protocol is not limited to user directories, and could be used to search other Jabber information repositories
 * (such as chatroom directories) or even to provide a Jabber interface to conventional search engines.
 * <p/>
 * The basic functionality is to query an information repository regarding the possible search fields, to send a search query, and to receive search results.
 *
 * @author Derek DeMoro
 */
public class UserSearch extends SimpleIQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:search";

    /**
     * Creates a new instance of UserSearch.
     */
    public UserSearch() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Returns the form for all search fields supported by the search service.
     *
     * @param con           the current XMPPConnection.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return the search form received by the server.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Form getSearchForm(XMPPConnection con, DomainBareJid searchService) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.get);
        search.setTo(searchService);

        IQ response = (IQ) con.createPacketCollectorAndSend(search).nextResultOrThrow();
        return Form.getFormFrom(response);
    }

    /**
     * Sends the filled out answer form to be sent and queried by the search service.
     *
     * @param con           the current XMPPConnection.
     * @param searchForm    the <code>Form</code> to send for querying.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return ReportedData the data found from the query.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public ReportedData sendSearchForm(XMPPConnection con, Form searchForm, DomainBareJid searchService) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.set);
        search.setTo(searchService);
        search.addExtension(searchForm.getDataFormToSend());

        IQ response = (IQ) con.createPacketCollectorAndSend(search).nextResultOrThrow();
        return ReportedData.getReportedDataFrom(response);
    }

    /**
     * Sends the filled out answer form to be sent and queried by the search service.
     *
     * @param con           the current XMPPConnection.
     * @param searchForm    the <code>Form</code> to send for querying.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return ReportedData the data found from the query.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public ReportedData sendSimpleSearchForm(XMPPConnection con, Form searchForm, DomainBareJid searchService) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        SimpleUserSearch search = new SimpleUserSearch();
        search.setForm(searchForm);
        search.setType(IQ.Type.set);
        search.setTo(searchService);

        SimpleUserSearch response = (SimpleUserSearch) con.createPacketCollectorAndSend(search).nextResultOrThrow();
        return response.getReportedData();
    }

    /**
     * Internal Search service Provider.
     */
    public static class Provider extends IQProvider<IQ> {

        // FIXME this provider does return two different types of IQs
        @Override
        public IQ parse(XmlPullParser parser, int initialDepth) throws Exception {
            UserSearch search = null;
            SimpleUserSearch simpleUserSearch = new SimpleUserSearch();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("instructions")) {
                    buildDataForm(simpleUserSearch, parser.nextText(), parser);
                    return simpleUserSearch;
                }
                else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("item")) {
                    simpleUserSearch.parseItems(parser);
                    return simpleUserSearch;
                }
                else if (eventType == XmlPullParser.START_TAG && parser.getNamespace().equals("jabber:x:data")) {
                    // Otherwise, it must be a packet extension.
                    search = new UserSearch();
                    PacketParserUtils.addExtensionElement(search, parser);
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("query")) {
                        done = true;
                    }
                }
            }

            if (search != null) {
                return search;
            }
            return simpleUserSearch;
        }
    }

    private static void buildDataForm(SimpleUserSearch search,
                    String instructions, XmlPullParser parser)
                    throws Exception {
        DataForm dataForm = new DataForm(DataForm.Type.form);
        boolean done = false;
        dataForm.setTitle("User Search");
        dataForm.addInstruction(instructions);
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && !parser.getNamespace().equals("jabber:x:data")) {
                String name = parser.getName();
                FormField field = new FormField(name);

                // Handle hard coded values.
                if(name.equals("first")){
                    field.setLabel("First Name");
                }
                else if(name.equals("last")){
                    field.setLabel("Last Name");
                }
                else if(name.equals("email")){
                    field.setLabel("Email Address");
                }
                else if(name.equals("nick")){
                    field.setLabel("Nickname");
                }

                field.setType(FormField.Type.text_single);
                dataForm.addField(field);
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
            else if (eventType == XmlPullParser.START_TAG && parser.getNamespace().equals("jabber:x:data")) {
                PacketParserUtils.addExtensionElement(search, parser);
                done = true;
            }
        }
        if (search.getExtension("x", "jabber:x:data") == null) {
            search.addExtension(dataForm);
        }
    }


}
