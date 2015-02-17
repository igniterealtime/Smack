/**
 *
 * Copyright 2015 Florian Schmaus
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

package org.jivesoftware.smack.roster;

/**
 * Roster loaded listeners are invoked once the {@link Roster} was successfully loaded.
 * <p>
 * A common approach is to call
 * {@link Roster#getEntriesAndAddListener(RosterListener, RosterEntries)} within
 * {@link #onRosterLoaded(Roster)}, to initialize or update your UI components with the current
 * roster state.
 * </p>
 */
public interface RosterLoadedListener {

    /**
     * Called when the Roster was loaded successfully.
     *
     * @param roster the Roster that was loaded successfully.
     */
    public void onRosterLoaded(Roster roster);

}
