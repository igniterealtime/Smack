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
package org.jivesoftware.smack.filters;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.junit.Test;

/**
 * 
 */
public class FilterToStringTest {

    @Test
    public void abstractListFilterToStringTest() {
        AndFilter andFilter = new AndFilter();
        andFilter.addFilter(new StanzaIdFilter("foo"));
        andFilter.addFilter(new ThreadFilter("42"));
        andFilter.addFilter(MessageWithBodiesFilter.INSTANCE);

        final String res =andFilter.toString();
        assertEquals("AndFilter: (StanzaIdFilter: id=foo, ThreadFilter: thread=42, MessageWithBodiesFilter)", res);
    }
}
