/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.disco;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;

import java.util.List;


public abstract class AbstractNodeInformationProvider implements NodeInformationProvider {

    @Override
    public List<DiscoverItems.Item> getNodeItems() {
        return null;
    }

    @Override
    public List<String> getNodeFeatures() {
        return null;
    }

    @Override
    public List<DiscoverInfo.Identity> getNodeIdentities() {
        return null;
    }

    @Override
    public List<ExtensionElement> getNodePacketExtensions() {
        return null;
    }

}
