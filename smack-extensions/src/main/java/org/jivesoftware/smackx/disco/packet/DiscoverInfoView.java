/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.disco.packet;

import java.util.List;

import org.jivesoftware.smack.packet.IqView;

public interface DiscoverInfoView extends IqView {

    /**
     * Returns the discovered features of an XMPP entity.
     *
     * @return an unmodifiable list of the discovered features of an XMPP entity
     */
    List<DiscoverInfo.Feature> getFeatures();

    /**
     * Returns the discovered identities of an XMPP entity.
     *
     * @return an unmodifiable list of the discovered identities
     */
    List<DiscoverInfo.Identity> getIdentities();

    /**
     * Returns the node attribute that supplements the 'jid' attribute. A node is merely
     * something that is associated with a JID and for which the JID can provide information.<p>
     *
     * Node attributes SHOULD be used only when trying to provide or query information which
     * is not directly addressable.
     *
     * @return the node attribute that supplements the 'jid' attribute
     */
    String getNode();
}
