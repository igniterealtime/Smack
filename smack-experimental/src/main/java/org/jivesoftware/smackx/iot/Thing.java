/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smackx.iot.control.ThingControlRequest;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutRequest;
import org.jivesoftware.smackx.iot.discovery.element.Tag;
import org.jivesoftware.smackx.iot.element.NodeInfo;

public final class Thing {

    private final HashMap<String, Tag> metaTags;
    private final boolean selfOwned;
    private final NodeInfo nodeInfo;

    private final ThingMomentaryReadOutRequest momentaryReadOutRequestHandler;
    private final ThingControlRequest controlRequestHandler;

    private Thing(Builder builder) {
        this.metaTags = builder.metaTags;
        this.selfOwned = builder.selfOwned;

        this.nodeInfo = builder.nodeInfo;

        this.momentaryReadOutRequestHandler = builder.momentaryReadOutRequest;
        this.controlRequestHandler = builder.controlRequest;
    }

    public Collection<Tag> getMetaTags() {
        return metaTags.values();
    }

    public boolean isSelfOwened() {
        return selfOwned;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public String getNodeId() {
        return nodeInfo.getNodeId();
    }

    public String getSourceId() {
        return nodeInfo.getSourceId();
    }

    public String getCacheType() {
        return nodeInfo.getCacheType();
    }

    public ThingMomentaryReadOutRequest getMomentaryReadOutRequestHandler() {
        return momentaryReadOutRequestHandler;
    }

    public ThingControlRequest getControlRequestHandler() {
        return controlRequestHandler;
    }

    private String toStringCache;

    @SuppressWarnings("ObjectToString")
    @Override
    public String toString() {
        if (toStringCache == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Thing " + nodeInfo + " [");
            Iterator<Tag> it = metaTags.values().iterator();
            while (it.hasNext()) {
                Tag tag = it.next();
                sb.append(tag);
                if (it.hasNext()) {
                    sb.append(' ');
                }
            }
            sb.append(']');
            toStringCache = sb.toString();
        }
        return toStringCache;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HashMap<String, Tag> metaTags = new HashMap<>();
        private boolean selfOwned;
        private NodeInfo nodeInfo = NodeInfo.EMPTY;
        private ThingMomentaryReadOutRequest momentaryReadOutRequest;
        private ThingControlRequest controlRequest;

        public Builder setSerialNumber(String sn) {
            final String name = "SN";
            Tag tag = new Tag(name, Tag.Type.str, sn);
            metaTags.put(name, tag);
            return this;
        }

        public Builder setKey(String key) {
            final String name = "KEY";
            Tag tag = new Tag(name, Tag.Type.str, key);
            metaTags.put(name, tag);
            return this;
        }

        public Builder setManufacturer(String manufacturer) {
            final String name = "MAN";
            Tag tag = new Tag(name, Tag.Type.str, manufacturer);
            metaTags.put(name, tag);
            return this;
        }

        public Builder setModel(String model) {
            final String name = "MODEL";
            Tag tag = new Tag(name, Tag.Type.str, model);
            metaTags.put(name, tag);
            return this;
        }

        public Builder setVersion(String version) {
            final String name = "V";
            Tag tag = new Tag(name, Tag.Type.num, version);
            metaTags.put(name, tag);
            return this;
        }

        public Builder setMomentaryReadOutRequestHandler(ThingMomentaryReadOutRequest momentaryReadOutRequestHandler) {
            this.momentaryReadOutRequest = momentaryReadOutRequestHandler;
            return this;
        }

        public Builder setControlRequestHandler(ThingControlRequest controlRequest) {
            this.controlRequest = controlRequest;
            return this;
        }

        public Thing build() {
            return new Thing(this);
        }
    }
}
