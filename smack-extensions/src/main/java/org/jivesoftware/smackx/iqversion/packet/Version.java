/**
 *
 * Copyright 2003-2007 Jive Software, 2021 Florian Schmaus.
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

package org.jivesoftware.smackx.iqversion.packet;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.util.StringUtils;

/**
 * A Version IQ packet, which is used by XMPP clients to discover version information
 * about the software running at another entity's JID.<p>
 *
 * @author Gaston Dombiak
 */
public class Version extends IQ implements VersionView {
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:version";

    private final String name;
    private final String version;
    private String os;

    Version(VersionBuilder versionBuilder) {
        super(versionBuilder, ELEMENT, NAMESPACE);
        name = versionBuilder.getName();
        version = versionBuilder.getVersion();
        os = versionBuilder.getOs();

        if (getType() != IQ.Type.result) {
            return;
        }
        StringUtils.requireNotNullNorEmpty(name, "Version results must contain a name");
        StringUtils.requireNotNullNorEmpty(version, "Version results must contain a version");
    }

    /**
     * Returns the natural-language name of the software. This property will always be
     * present in a result.
     *
     * @return the natural-language name of the software.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the specific version of the software. This property will always be
     * present in a result.
     *
     * @return the specific version of the software.
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Returns the operating system of the queried entity. This property will always be
     * present in a result.
     *
     * @return the operating system of the queried entity.
     */
    @Override
    public String getOs() {
        return os;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        // Although not really optional elements, 'name' and 'version' are not set when sending a
        // version request. So we must handle the case that those are 'null' here.
        xml.optElement("name", name);
        xml.optElement("version", version);
        xml.optElement("os", os);
        return xml;
    }

    public static VersionBuilder builder(XMPPConnection connection) {
        return new VersionBuilder(connection);
    }

    public static VersionBuilder builder(IqData iqData) {
        return new VersionBuilder(iqData);
    }

    public static VersionBuilder builder(Version versionRequest) {
        IqData iqData = IqData.createResponseData(versionRequest);
        return builder(iqData);
    }
}
