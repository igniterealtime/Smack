/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.muclight.element;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.ConfigurationElement;

/**
 * MUC Light configuration response IQ class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightConfigurationIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.CONFIGURATION;

    private final String version;
    private final MUCLightRoomConfiguration configuration;

    /**
     * MUC Light configuration response IQ constructor.
     *
     * @param version TODO javadoc me please
     * @param configuration TODO javadoc me please
     */
    public MUCLightConfigurationIQ(String version, MUCLightRoomConfiguration configuration) {
        super(ELEMENT, NAMESPACE);
        this.version = version;
        this.configuration = configuration;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("version", version);
        xml.element(new ConfigurationElement(configuration));
        return xml;
    }

    /**
     * Returns the version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the room configuration.
     *
     * @return the configuration of the room
     */
    public MUCLightRoomConfiguration getConfiguration() {
        return configuration;
    }

}
