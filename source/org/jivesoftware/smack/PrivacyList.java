/**
 * $RCSfile$
 * $Revision$
 * $Date$
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
 * A privacy list represents a list of contacts that is a read only class used to represent a set of allowed or blocked communications. 
 * Basically it can:<ul>
 *
 *      <li>Handle many {@link org.jivesoftware.smack.packet.PrivacyItem}.</li>
 *      <li>Answer if it is the default list.</li>
 *      <li>Answer if it is the active list.</li>
 * </ul>
 *
 * {@link PrivacyItem Privacy Items} can handle different kind of blocking communications based on JID, group,
 * subscription type or globally.
 * 
 * @author Francisco Vives
 */
public class PrivacyList {

    /** Holds if it is an active list or not **/
    private boolean isActiveList;
    /** Holds if it is an default list or not **/
    private boolean isDefaultList;
    /** Holds the list name used to print **/
    private String listName;
    /** Holds the list of {@see PrivacyItem} **/
    private List<PrivacyItem> items;
    
    protected PrivacyList(boolean isActiveList, boolean isDefaultList,
            String listName, List<PrivacyItem> privacyItems) {
        super();
        this.isActiveList = isActiveList;
        this.isDefaultList = isDefaultList;
        this.listName = listName;
        this.items = privacyItems;
    }

    public boolean isActiveList() {
        return isActiveList;
    }

    public boolean isDefaultList() {
        return isDefaultList;
    }

    public List<PrivacyItem> getItems() {
        return items;
    }

    public String toString() {
        return listName;
    }

}
