/**
 * $Revision$
 * $Date$
 *
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

package org.jivesoftware.smackx.workgroup.agent;

import org.jivesoftware.smackx.workgroup.packet.TranscriptSearch;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;

/**
 * A TranscriptSearchManager helps to retrieve the form to use for searching transcripts
 * {@link #getSearchForm(String)} or to submit a search form and return the results of
 * the search {@link #submitSearch(String, Form)}.
 *
 * @author Gaston Dombiak
 */
public class TranscriptSearchManager {
    private XMPPConnection connection;

    public TranscriptSearchManager(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Returns the Form to use for searching transcripts. It is unlikely that the server
     * will change the form (without a restart) so it is safe to keep the returned form
     * for future submissions.
     *
     * @param serviceJID the address of the workgroup service.
     * @return the Form to use for searching transcripts.
     * @throws XMPPException if an error occurs while sending the request to the server.
     */
    public Form getSearchForm(String serviceJID) throws XMPPException {
        TranscriptSearch search = new TranscriptSearch();
        search.setType(IQ.Type.GET);
        search.setTo(serviceJID);

        PacketCollector collector = connection.createPacketCollector(
                new PacketIDFilter(search.getPacketID()));
        connection.sendPacket(search);

        TranscriptSearch response = (TranscriptSearch) collector.nextResult(
                SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return Form.getFormFrom(response);
    }

    /**
     * Submits the completed form and returns the result of the transcript search. The result
     * will include all the data returned from the server so be careful with the amount of
     * data that the search may return.
     *
     * @param serviceJID    the address of the workgroup service.
     * @param completedForm the filled out search form.
     * @return the result of the transcript search.
     * @throws XMPPException if an error occurs while submiting the search to the server.
     */
    public ReportedData submitSearch(String serviceJID, Form completedForm) throws XMPPException {
        TranscriptSearch search = new TranscriptSearch();
        search.setType(IQ.Type.GET);
        search.setTo(serviceJID);
        search.addExtension(completedForm.getDataFormToSend());

        PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(search.getPacketID()));
        connection.sendPacket(search);

        TranscriptSearch response = (TranscriptSearch) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return ReportedData.getReportedDataFrom(response);
    }
}


