/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.workgroup.packet;

import java.util.List;
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
public class MetaDataProvider implements PacketExtensionProvider {

    /**
     * PacketExtensionProvider implementation
     */
    public PacketExtension parseExtension (XmlPullParser parser)
        throws Exception {
        Map<String, List<String>> metaData = MetaDataUtils.parseMetaData(parser);

        return new MetaData(metaData);
    }
}