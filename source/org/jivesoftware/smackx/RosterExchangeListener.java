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

package org.jivesoftware.smackx;

import java.util.Iterator;

/**
 *
 * A listener that is fired anytime a roster exchange is received.
 *
 * @author Gaston Dombiak
 */
public interface RosterExchangeListener {

    /**
     * Called when roster entries are received as part of a roster exchange.
     *  
     * @param from the user that sent the entries.
     * @param remoteRosterEntries the entries sent by the user. The entries are instances of 
     * RemoteRosterEntry.
     */
    public void entriesReceived(String from, Iterator remoteRosterEntries);

}
