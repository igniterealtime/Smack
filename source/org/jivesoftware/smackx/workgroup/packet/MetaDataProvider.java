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

import org.jivesoftware.smackx.workgroup.MetaData;
import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;

import org.xmlpull.v1.XmlPullParser;


/**
 * This provider parses meta data if it's not contained already in a larger extension provider.
 *
 * @author loki der quaeler
 */
public class MetaDataProvider
    implements PacketExtensionProvider {


    /**
     * PacketExtensionProvider implementation
     */
    public PacketExtension parseExtension (XmlPullParser parser)
        throws Exception {
        Map metaData = MetaDataUtils.parseMetaData(parser);

        return new MetaData(metaData);
    }

}
