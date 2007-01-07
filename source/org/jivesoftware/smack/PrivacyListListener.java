/**
 * $Revision$
 * $Date$
 *
 * Copyright 2006-2007 Jive Software.
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

import org.jivesoftware.smack.packet.PrivacyItem;

import java.util.List;

/**
 * Interface to implement classes to listen for server events about privacy communication. 
 * Listeners are registered with the {@link PrivacyListManager}.
 *
 * @see PrivacyListManager#addListener
 * 
 * @author Francisco Vives
 */
public interface PrivacyListListener {

    /**
     * Set or update a privacy list with PrivacyItem.
     *
     * @param listName the name of the new or updated privacy list.
     * @param listItem the PrivacyItems that rules the list.
     */
    public void setPrivacyList(String listName, List<PrivacyItem> listItem);

    /**
     * A privacy list has been modified by another. It gets notified.
     *
     * @param listName the name of the updated privacy list.
     */
    public void updatedPrivacyList(String listName);

}