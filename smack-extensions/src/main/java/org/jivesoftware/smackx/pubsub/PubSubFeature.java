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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smackx.disco.Feature;
import org.jivesoftware.smackx.disco.Feature.Support;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

/**
 * The features a PubSub service may provides. Some are optional or recommended, while others are required.
 *
 * @author Florian Schmaus
 * @see <a href="http://www.xmpp.org/extensions/xep-0060.html#features">XEP-60 § 10</a>
 *
 */
public enum PubSubFeature implements CharSequence {

    access_authorize(Support.optional),
    access_open(Support.optional),
    access_presence(Support.optional),
    access_roster(Support.optional),
    acccess_whitelist(Support.optional),
    auto_create(Support.optional),
    auto_subscribe(Support.recommended),
    collections(Support.optional),
    config_node(Support.recommended),
    create_and_configure(Support.recommended),
    create_nodes(Support.recommended),
    delete_items(Support.recommended),
    delete_nodes(Support.recommended),
    get_pending(Support.optional),
    item_ids(Support.recommended),
    last_published(Support.recommended),
    leased_subscription(Support.optional),
    manage_subscriptions(Support.optional),
    member_affiliation(Support.recommended),
    meta_data(Support.recommended),
    modify_affiliations(Support.optional),
    multi_collection(Support.optional),
    multi_subscribe(Support.optional),
    outcast_affiliation(Support.recommended),
    persistent_items(Support.recommended),
    presence_notifications(Support.optional),
    presence_subscribe(Support.recommended),
    publish(Support.required),
    publish_options(Support.optional),
    publish_only_affiliation(Support.optional),
    publisher_affiliation(Support.recommended),
    purge_nodes(Support.optional),
    retract_items(Support.optional),
    retrieve_affiliations(Support.recommended),
    retrieve_default(Support.recommended),
    retrieve_default_sub(Support.optional),
    retrieve_items(Support.recommended),
    retrieve_subscriptions(Support.recommended),
    subscribe(Support.required),
    subscription_options(Support.optional),
    subscriptions_notifications(Support.optional),
    instant_nodes(Support.recommended),
    filtered_notifications(Support.recommended),
    ;

    private final String feature;
    private final String qualifiedFeature;
    private final Feature.Support support;

    private PubSubFeature(Feature.Support support) {
        this.feature = name().replace('_', '-');
        this.qualifiedFeature = PubSub.NAMESPACE + '#' + this.feature;
        this.support = support;
    }

    public String getFeatureName() {
        return feature;
    }

    @Override
    public String toString() {
        return qualifiedFeature;
    }

    public Feature.Support support() {
        return support;
    }

    @Override
    public int length() {
        return qualifiedFeature.length();
    }

    @Override
    public char charAt(int index) {
        return qualifiedFeature.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return qualifiedFeature.subSequence(start, end);
    }

}
