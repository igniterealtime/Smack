/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Stanza;

/**
 * Implements the logical OR operation over two or more packet filters. In
 * other words, packets pass this filter if they pass <b>any</b> of the filters.
 *
 * @author Matt Tucker
 */
public class OrFilter extends AbstractListFilter implements StanzaFilter {

    /**
     * Creates an empty OR filter. Filters should be added using the
     * {@link #addFilter(StanzaFilter)} method.
     */
    public OrFilter() {
        super();
    }

    /**
     * Creates an OR filter using the specified filters.
     *
     * @param filters the filters to add.
     */
    public OrFilter(StanzaFilter... filters) {
        super(filters);
    }

    @Override
    public boolean accept(Stanza packet) {
        for (StanzaFilter filter : filters) {
            if (filter.accept(packet)) {
                return true;
            }
        }
        return false;
    }

}
