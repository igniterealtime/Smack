/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub.filter;

import org.jivesoftware.smack.filter.ExtensionElementFilter;

import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;

public final class EventItemsExtensionFilter extends ExtensionElementFilter<EventElement> {

    public static final EventItemsExtensionFilter INSTANCE = new EventItemsExtensionFilter();

    private EventItemsExtensionFilter() {
        super(EventElement.class);
    }

    @Override
    public boolean accept(EventElement eventElement) {
        EventElementType eventElementType = eventElement.getEventType();
        return eventElementType == EventElementType.items;
    }
}
