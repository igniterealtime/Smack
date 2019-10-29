/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.disco.packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.IqData;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Feature;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;

public class DiscoverInfoBuilder extends IqBuilder<DiscoverInfoBuilder, DiscoverInfo>
        implements DiscoverInfoView {

    private final List<Feature> features = new ArrayList<>();
    private final List<Identity> identities = new ArrayList<>();

    private String node;

    DiscoverInfoBuilder(IqData iqCommon) {
        super(iqCommon);
    }

    DiscoverInfoBuilder(XMPPConnection connection) {
        super(connection);
    }

    DiscoverInfoBuilder(String stanzaId) {
        super(stanzaId);
    }

    public DiscoverInfoBuilder(DiscoverInfo discoverInfo) {
        super(discoverInfo.getStanzaId());
        features.addAll(discoverInfo.getFeatures());
        identities.addAll(discoverInfo.getIdentities());
        node = discoverInfo.getNode();
    }

    @Override
    public DiscoverInfoBuilder getThis() {
        return this;
    }

    public DiscoverInfoBuilder addFeatures(Collection<String> features) {
        for (String feature : features) {
            addFeature(feature);
        }
        return getThis();
    }

    public DiscoverInfoBuilder addFeature(String feature) {
        return addFeature(new Feature(feature));
    }

    public DiscoverInfoBuilder addFeature(Feature feature) {
        features.add(feature);
        return getThis();
    }

    public DiscoverInfoBuilder addIdentities(Collection<Identity> identities) {
        this.identities.addAll(identities);
        return getThis();
    }

    public DiscoverInfoBuilder addIdentity(Identity identity) {
        identities.add(identity);
        return getThis();
    }

    public DiscoverInfoBuilder setNode(String node) {
        this.node = node;
        return getThis();
    }

    @Override
    public DiscoverInfo build() {
        return new DiscoverInfo(this, true);
    }

    public DiscoverInfo buildWithoutValidiation() {
        return new DiscoverInfo(this, false);
    }

    @Override
    public List<Feature> getFeatures() {
        return features;
    }

    @Override
    public List<Identity> getIdentities() {
        return identities;
    }

    @Override
    public String getNode() {
        return node;
    }

    public static DiscoverInfoBuilder buildResponseFor(DiscoverInfo request, IQ.ResponseType responseType) {
        DiscoverInfoBuilder builder = new DiscoverInfoBuilder(createResponse(request, responseType));
        builder.setNode(request.getNode());
        return builder;
    }
}
