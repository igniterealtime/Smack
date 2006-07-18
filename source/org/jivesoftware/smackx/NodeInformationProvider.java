/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smackx.packet.DiscoverItems;

import java.util.Iterator;


/**
 * The NodeInformationProvider is responsible for providing supported features and hosted
 * items (i.e. DiscoverItems.Item) about a given node. This information will be requested
 * each time this XMPPP client receives a disco info or items requests on the given node.
 * each time this XMPPP client receives a disco info or items requests on the given node.
 *
 * @author Gaston Dombiak
 */
public interface NodeInformationProvider {
    
    /**
     * Returns an Iterator on the Items {@link org.jivesoftware.smackx.packet.DiscoverItems.Item} 
     * defined in the node. For example, the MUC protocol specifies that an XMPP client should 
     * answer an Item for each joined room when asked for the rooms where the use has joined.
     *  
     * @return an Iterator on the Items defined in the node.
     */
    public abstract Iterator<DiscoverItems.Item> getNodeItems();

    /**
     * Returns an Iterator on the features defined in the node. For
     * example, the entity caps protocol specifies that an XMPP client
     * should answer with each feature supported by the client version
     * or extension.
     *
     * @return an Iterator on the feature strings defined in the node.
     */
    public abstract Iterator<String> getNodeFeatures();
}
