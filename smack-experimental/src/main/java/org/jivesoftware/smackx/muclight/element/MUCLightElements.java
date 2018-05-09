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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

public abstract class MUCLightElements {

    /**
     * Affiliations change extension element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class AffiliationsChangeExtension implements ExtensionElement {

        public static final String ELEMENT = DataForm.ELEMENT;
        public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.AFFILIATIONS;

        private final HashMap<Jid, MUCLightAffiliation> affiliations;
        private final String prevVersion;
        private final String version;

        public AffiliationsChangeExtension(HashMap<Jid, MUCLightAffiliation> affiliations, String prevVersion,
                String version) {
            this.affiliations = affiliations;
            this.prevVersion = prevVersion;
            this.version = version;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        /**
         * Get the affiliations.
         *
         * @return the affiliations
         */
        public HashMap<Jid, MUCLightAffiliation> getAffiliations() {
            return affiliations;
        }

        /**
         * Get the previous version.
         *
         * @return the previous version
         */
        public String getPrevVersion() {
            return prevVersion;
        }

        /**
         * Get the version.
         *
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();

            xml.optElement("prev-version", prevVersion);
            xml.optElement("version", version);

            Iterator<Map.Entry<Jid, MUCLightAffiliation>> it = affiliations.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Jid, MUCLightAffiliation> pair = it.next();
                xml.element(new UserWithAffiliationElement(pair.getKey(), pair.getValue()));
            }

            xml.closeElement(this);
            return xml;
        }

        public static AffiliationsChangeExtension from(Message message) {
            return message.getExtension(AffiliationsChangeExtension.ELEMENT, AffiliationsChangeExtension.NAMESPACE);
        }

    }

    /**
     * Configurations change extension element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class ConfigurationsChangeExtension implements ExtensionElement {

        public static final String ELEMENT = DataForm.ELEMENT;
        public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.CONFIGURATION;

        private final String prevVersion;
        private final String version;
        private final String roomName;
        private final String subject;
        private final HashMap<String, String> customConfigs;

        /**
         * Configurations change extension constructor.
         *
         * @param prevVersion
         * @param version
         * @param roomName
         * @param subject
         * @param customConfigs
         */
        public ConfigurationsChangeExtension(String prevVersion, String version, String roomName, String subject,
                HashMap<String, String> customConfigs) {
            this.prevVersion = prevVersion;
            this.version = version;
            this.roomName = roomName;
            this.subject = subject;
            this.customConfigs = customConfigs;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        /**
         * Get the previous version.
         *
         * @return the previous version
         */
        public String getPrevVersion() {
            return prevVersion;
        }

        /**
         * Get the version.
         *
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        /**
         * Get the room name.
         *
         * @return the room name
         */
        public String getRoomName() {
            return roomName;
        }

        /**
         * Get the room subject.
         *
         * @return the room subject
         */
        public String getSubject() {
            return subject;
        }

        /**
         * Get the room custom configurations.
         *
         * @return the room custom configurations
         */
        public HashMap<String, String> getCustomConfigs() {
            return customConfigs;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();

            xml.optElement("prev-version", prevVersion);
            xml.optElement("version", version);
            xml.optElement("roomname", roomName);
            xml.optElement("subject", subject);

            if (customConfigs != null) {
                Iterator<Map.Entry<String, String>> it = customConfigs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    xml.element(pair.getKey(), pair.getValue());
                }
            }

            xml.closeElement(this);
            return xml;
        }

        public static ConfigurationsChangeExtension from(Message message) {
            return message.getExtension(ConfigurationsChangeExtension.ELEMENT, ConfigurationsChangeExtension.NAMESPACE);
        }

    }

    /**
     * Configuration element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class ConfigurationElement implements Element {

        private MUCLightRoomConfiguration configuration;

        /**
         * Configuration element constructor.
         *
         * @param configuration
         */
        public ConfigurationElement(MUCLightRoomConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.openElement("configuration");

            xml.element("roomname", configuration.getRoomName());
            xml.optElement("subject", configuration.getSubject());

            if (configuration.getCustomConfigs() != null) {
                Iterator<Map.Entry<String, String>> it = configuration.getCustomConfigs().entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    xml.element(pair.getKey(), pair.getValue());
                }
            }

            xml.closeElement("configuration");
            return xml;
        }

    }

    /**
     * Occupants element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class OccupantsElement implements Element {

        private HashMap<Jid, MUCLightAffiliation> occupants;

        /**
         * Occupants element constructor.
         *
         * @param occupants
         */
        public OccupantsElement(HashMap<Jid, MUCLightAffiliation> occupants) {
            this.occupants = occupants;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.openElement("occupants");

            Iterator<Map.Entry<Jid, MUCLightAffiliation>> it = occupants.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Jid, MUCLightAffiliation> pair = it.next();
                xml.element(new UserWithAffiliationElement(pair.getKey(), pair.getValue()));
            }

            xml.closeElement("occupants");
            return xml;
        }

    }

    /**
     * User with affiliation element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class UserWithAffiliationElement implements Element {

        private Jid user;
        private MUCLightAffiliation affiliation;

        /**
         * User with affiliations element constructor.
         *
         * @param user
         * @param affiliation
         */
        public UserWithAffiliationElement(Jid user, MUCLightAffiliation affiliation) {
            this.user = user;
            this.affiliation = affiliation;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement("user");
            xml.attribute("affiliation", affiliation);
            xml.rightAngleBracket();
            xml.escape(user);
            xml.closeElement("user");
            return xml;
        }

    }

    /**
     * Blocking element class.
     *
     * @author Fernando Ramirez
     *
     */
    public static class BlockingElement implements Element {

        private Jid jid;
        private Boolean allow;
        private Boolean isRoom;

        /**
         * Blocking element constructor.
         *
         * @param jid
         * @param allow
         * @param isRoom
         */
        public BlockingElement(Jid jid, Boolean allow, Boolean isRoom) {
            this.jid = jid;
            this.allow = allow;
            this.isRoom = isRoom;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();

            String tag = isRoom ? "room" : "user";
            xml.halfOpenElement(tag);

            String action = allow ? "allow" : "deny";
            xml.attribute("action", action);
            xml.rightAngleBracket();

            xml.escape(jid);

            xml.closeElement(tag);
            return xml;
        }

    }

}
