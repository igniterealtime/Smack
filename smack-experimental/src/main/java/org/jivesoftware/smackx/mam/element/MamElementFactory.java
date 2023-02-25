/**
 *
 * Copyright © 2016-2021 Florian Schmaus and Frank Matheron
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
package org.jivesoftware.smackx.mam.element;

import java.util.List;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
 * Factory that creates MAM objects.
 *
 * @since 4.5.0
 */
public interface MamElementFactory {

    /**
     * Creates a new {@link MamElementFactory} for the parser based on the namespace of the parser.
     * @param parser the XML parser to retrieve the MAM namespace from
     * @return the factory suitable for the MAM namespace
     */
    static MamElementFactory forParser(XmlPullParser parser) {
        String namespace = parser.getNamespace();
        return MamVersion.fromNamespace(namespace).newElementFactory();
    }

    /**
     * Create a MAM result extension class.
     *
     * @param queryId id of the query
     * @param id the message's archive UID
     * @param forwarded the original message as it was received
     * @return the result extension
     */
    MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded);

    /**
     * Create a MAM fin IQ class.
     *
     * @param queryId id of the query
     * @param rsmSet the RSM set included in the {@code <fin/>}
     * @param complete true if the results returned by the server are complete (no further paging in needed)
     * @param stable false if the results returned by the sever are unstable (e.g. they might later change in sequence or content)
     * @return the fin IQ
     */
    MamFinIQ newFinIQ(String queryId, RSMSet rsmSet, boolean complete, boolean stable);

    /**
     * Create a new MAM preferences IQ.
     *
     * @param alwaysJids JIDs for which all messages are archived by default
     * @param neverJids JIDs for which messages are never archived
     * @param defaultBehavior default archive behavior
     * @return the prefs IQ
     */
    MamPrefsIQ newPrefsIQ(List<Jid> alwaysJids, List<Jid> neverJids, MamPrefsIQ.DefaultBehavior defaultBehavior);

    /**
     * Construct a new MAM {@code <prefs/>} IQ retrieval request (IQ type 'get').
     *
     * @return the prefs IQ
     */
    MamPrefsIQ newPrefsIQ();

    /**
     * Create a new MAM Query IQ.
     *
     * @param queryId id of the query
     * @param node pubsub node id when querying a pubsub node, null when not querying a pubsub node
     * @param dataForm the dataform containing the query parameters
     * @return the query IQ
     */
    MamQueryIQ newQueryIQ(String queryId, String node, DataForm dataForm);

}
