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
package org.jivesoftware.smackx.pubsub.form;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smackx.pubsub.PresenceState;
import org.jivesoftware.smackx.pubsub.SubscribeOptionFields;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.form.FormReader;

public interface SubscribeFormReader extends FormReader {

    String FORM_TYPE = PubSub.NAMESPACE + "#subscribe_options";

    /**
     * Determines if an entity wants to receive notifications.
     *
     * @return true if want to receive, false otherwise
     */
    default boolean isDeliverOn() {
        return readBoolean(SubscribeOptionFields.deliver.getFieldName());
    }

    /**
     * Determines if notifications should be delivered as aggregations or not.
     *
     * @return true to aggregate, false otherwise
     */
    default Boolean isDigestOn() {
        return readBoolean(SubscribeOptionFields.digest.getFieldName());
    }

    /**
     * Gets the minimum number of milliseconds between sending notification digests.
     *
     * @return The frequency in milliseconds
     */
    default Integer getDigestFrequency() {
        return readInteger(SubscribeOptionFields.digest_frequency.getFieldName());
    }

    /**
     * Get the time at which the leased subscription will expire, or has expired.
     *
     * @return The expiry date
     * @throws ParseException in case the date could not be parsed.
     */
    default Date getExpiry() throws ParseException {
        return readDate(SubscribeOptionFields.expire.getFieldName());
    }

    /**
     * Determines whether the entity wants to receive an XMPP message body in
     * addition to the payload format.
     *
     * @return true to receive the message body, false otherwise
     */
    default Boolean isIncludeBody() {
        return readBoolean(SubscribeOptionFields.include_body.getFieldName());
    }

    /**
     * Gets the {@link PresenceState} for which an entity wants to receive
     * notifications.
     *
     * @return the list of states
     */
    default List<PresenceState> getShowValues() {
        List<String> values = readStringValues(SubscribeOptionFields.show_values.getFieldName());
        List<PresenceState> result = new ArrayList<>(values.size());

        for (String state : values) {
            result.add(PresenceState.valueOf(state));
        }
        return result;
    }
}
