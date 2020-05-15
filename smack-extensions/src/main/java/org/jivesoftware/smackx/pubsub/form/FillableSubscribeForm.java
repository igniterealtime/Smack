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

import java.util.Collection;
import java.util.Date;

import org.jivesoftware.smackx.pubsub.PresenceState;
import org.jivesoftware.smackx.pubsub.SubscribeOptionFields;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.ListMultiFormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class FillableSubscribeForm extends FillableForm implements SubscribeFormReader {

    FillableSubscribeForm(DataForm dataForm) {
        super(dataForm);
    }

    /**
     * Sets whether an entity wants to receive notifications.
     *
     * @param deliverNotifications TODO javadoc me please
     */
    public void setDeliverOn(boolean deliverNotifications) {
        writeBoolean(SubscribeOptionFields.deliver.getFieldName(), deliverNotifications);
    }

    /**
     * Sets whether notifications should be delivered as aggregations or not.
     *
     * @param digestOn true to aggregate, false otherwise
     */
    public void setDigestOn(boolean digestOn) {
        writeBoolean(SubscribeOptionFields.digest.getFieldName(), digestOn);
    }

    /**
     * Sets the minimum number of milliseconds between sending notification digests.
     *
     * @param frequency The frequency in milliseconds
     */
    public void setDigestFrequency(int frequency) {
        write(SubscribeOptionFields.digest_frequency.getFieldName(), frequency);
    }

    /**
     * Sets the time at which the leased subscription will expire, or has expired.
     *
     * @param expire The expiry date
     */
    public void setExpiry(Date expire) {
        write(SubscribeOptionFields.expire.getFieldName(), expire);
    }

    /**
     * Sets whether the entity wants to receive an XMPP message body in
     * addition to the payload format.
     *
     * @param include true to receive the message body, false otherwise
     */
    public void setIncludeBody(boolean include) {
        writeBoolean(SubscribeOptionFields.include_body.getFieldName(), include);
    }

    /**
     * Sets the list of {@link PresenceState} for which an entity wants
     * to receive notifications.
     *
     * @param stateValues The list of states
     */
    public void setShowValues(Collection<PresenceState> stateValues) {
        ListMultiFormField.Builder builder = FormField.listMultiBuilder(SubscribeOptionFields.show_values.getFieldName());
        for (PresenceState state : stateValues) {
            builder.addValue(state.toString());
        }

        write(builder.build());
    }
}
