/*
 *
 * Copyright © 2011-2014 Florian Schmaus
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
package org.jivesoftware.smackx.caps.cache;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

public interface EntityCapsPersistentCache {
    /**
     * Add an DiscoverInfo to the persistent Cache.
     *
     * @param nodeVer TODO javadoc me please
     * @param info TODO javadoc me please
     */
    void addDiscoverInfoByNodePersistent(String nodeVer, DiscoverInfo info);

    /**
     * Lookup DiscoverInfo by a Node string.
     * @param nodeVer TODO javadoc me please
     *
     * @return DiscoverInfo.
     */
    DiscoverInfo lookup(String nodeVer);

    /**
     * Empty the Cache.
     */
    void emptyCache();
}
