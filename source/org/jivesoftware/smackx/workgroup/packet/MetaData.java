/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.packet;

import java.util.Map;

import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * MetaData packet extension.
 */
public class MetaData implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "metadata";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://www.jivesoftware.com/workgroup/metadata";

    private Map metaData;

    public MetaData(Map metaData) {
        this.metaData = metaData;
    }

    /**
     * @return the Map of metadata contained by this instance
     */
    public Map getMetaData() {
        return metaData;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        return MetaDataUtils.encodeMetaData(this.getMetaData());
    }
}