/**
 * $RCSfile$
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
package org.jivesoftware.smack;

import java.util.List;

import org.jivesoftware.smack.packet.Privacy;
import org.jivesoftware.smack.packet.PrivacyItem;

/**
 * This class supports automated tests about privacy communication from the
 * server to the client.
 * 
 * @author Francisco Vives
 */

public class PrivacyClient implements PrivacyListListener {
    /**
     * holds if the receiver list was modified
     */
    private boolean wasModified = false;

    /**
     * holds a privacy to hold server requests Clients should not use Privacy
     * class since it is private for the smack framework.
     */
    private Privacy privacy = new Privacy();

    public PrivacyClient(PrivacyListManager manager) {
        super();
    }

    public void setPrivacyList(String listName, List<PrivacyItem> listItem) {
        privacy.setPrivacyList(listName, listItem);
    }

    public void updatedPrivacyList(String listName) {
        this.wasModified = true;
    }

    public boolean wasModified() {
        return this.wasModified;
    }
}
