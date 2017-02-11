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
package org.jivesoftware.smackx.iot.element;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public final class NodeInfo {

    public static final NodeInfo EMPTY = new NodeInfo();

    private final String nodeId;
    private final String sourceId;
    private final String cacheType;

    /**
     * The internal constructor for the {@link EMPTY} node info marker class.
     */
    private NodeInfo() {
        this.nodeId = null;
        this.sourceId = null;
        this.cacheType = null;
    }

    public NodeInfo(String nodeId, String sourceId, String cacheType) {
        this.nodeId = StringUtils.requireNotNullOrEmpty(nodeId, "Node ID must not be null or empty");
        this.sourceId = sourceId;
        this.cacheType = cacheType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getCacheType() {
        return cacheType;
    }

    public void appendTo(XmlStringBuilder xml) {
        if (nodeId == null) {
            return;
        }
        xml.attribute("nodeId", nodeId).optAttribute("sourceId", sourceId).optAttribute("cacheType", cacheType);
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public int hashCode() {
        if (this == EMPTY) {
            return 0;
        }
        final int prime = 31;
        int result = 1;
        result = prime * result + nodeId.hashCode();
        result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
        result = prime * result + ((cacheType == null) ? 0 : cacheType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof NodeInfo)) {
            return false;
        }
        NodeInfo otherNodeInfo = (NodeInfo) other;
        if (!nodeId.equals(otherNodeInfo.nodeId)) {
            return false;
        }
        if (StringUtils.nullSafeCharSequenceEquals(sourceId, otherNodeInfo.sourceId)
                        && StringUtils.nullSafeCharSequenceEquals(cacheType, otherNodeInfo.cacheType)) {
            return true;
        }
        return false;
    }

//    public static void eventuallyAppend(NodeInfo nodeInfo, XmlStringBuilder xml) {
//        if (nodeInfo == null)
//            return;
//
//        nodeInfo.appendTo(xml);
//    }
}
