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

import java.util.*;
import java.beans.PropertyDescriptor;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;
import org.jivesoftware.smackx.workgroup.agent.Agent;


/**
 * An immutable container around the basic data known about a given agent, as well as a
 *  PacketExtension implementor which knows how to write the agent-status packet extension.
 *  The packet extension implementation doesn't transmit its presence information as, due to
 *  the protocol design, that is to be done by the presence packet which houses this extension.
 *  Similarly, the agent-status packet extension transmits nothing about the agent id, nor
 *  the metadata.<br>
 *
 * PENDING: feels hacky to carry around the presence packet, but it's an adequate container
 *  for the time being.<br>
 *
 * @author loki der quaeler
 */
public class AgentStatus implements PacketExtension {

     /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "agent-status";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "xmpp:workgroup";

    private Set agents;

    AgentStatus() {
        agents = new HashSet();
    }

    void addAgent(Agent agent) {
        synchronized (agents) {
            agents.add(agent);
        }
    }

    public int getAgentCount() {
        synchronized (agents) {
            return agents.size();
        }
    }

    public Set getAgents() {
        synchronized (agents) {
            return Collections.unmodifiableSet(agents);
        }
    }

    public String getElementName () {
        return ELEMENT_NAME;
    }

    public String getNamespace () {
        return NAMESPACE;
    }

    public String toXML () {
        StringBuffer buf = new StringBuffer();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");

        synchronized (agents) {
            for (Iterator i=agents.iterator(); i.hasNext(); ) {
                Agent agent = (Agent)i.next();
                buf.append("<agent jid=\"").append(agent.getUser()).append("\">");

                if (agent.getCurrentChats() != -1) {
                    buf.append("<current-chats>");
                    buf.append(agent.getCurrentChats());
                    buf.append("</current-chats>");
                }

                if (agent.getMaxChats() != -1) {
                    buf.append("<max-chats>").append(agent.getMaxChats()).append("</max-chats>");
                }

                if (agent.getPresence() != null) {
                    buf.append(agent.getPresence().toXML());
                    // TODO: ensure that presence.toXML method is ok, then delete code below.
                    /*Presence presence = agent.getPresence();
                    int priority = presence.getPriority();
                    Presence.Mode mode = presence.getMode();
                    String status = presence.getStatus();

                    buf.append("<presence>");
                    if (status != null) {
                        buf.append("<status>").append(status).append("</status>");
                    }
                    if (priority != -1) {
                        buf.append("<priority>").append(priority).append("</priority>");
                    }
                    if (mode != null && mode != Presence.Mode.AVAILABLE) {
                        buf.append("<show>").append(mode).append("</show>");
                    }
                    buf.append("</presence>");*/
                }

                buf.append("</agent>");
            }
        }

        buf.append("</").append(this.getElementName()).append("> ");

        return buf.toString();
    }

    /**
     * Packet extension provider for AgentStatus packets.
     */
    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            AgentStatus agentStatus = new AgentStatus();

            int eventType = parser.getEventType();
            if (eventType != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            eventType = parser.next();

            while ((eventType == XmlPullParser.START_TAG)
                            && ("agent".equals(parser.getName()))) {
                String jid = null;
                int currentChats = -1;
                int maxChats = -1;
                Presence presence = null;

                jid = parser.getAttributeValue("", "jid");
                if (jid == null) {
                    // throw exception
                }

                eventType = parser.next();
                String elementName = parser.getName();
                while ((eventType != XmlPullParser.END_TAG) || (!"agent".equals(elementName))) {
                    if ("current-chats".equals(elementName)) {
                        currentChats = Integer.parseInt(parser.nextText());
                        parser.next();
                    }
                    else if ("max-chats".equals(elementName)) {
                        maxChats = Integer.parseInt(parser.nextText());
                        parser.next();
                    }
                    else if ("presence".equals(elementName)) {
                        presence = parsePresence(parser);
                        parser.next();
                    }

                    eventType = parser.getEventType();
                    elementName = parser.getName();

                    if (eventType != XmlPullParser.END_TAG) {
                        // throw exception
                    }
                }

                Agent agent = new Agent(jid, currentChats, maxChats, presence);
                agentStatus.addAgent(agent);

                eventType = parser.next();
            }

            if (eventType != XmlPullParser.END_TAG) {
                // throw exception -- PENDING logic verify: useless case?
            }

            return agentStatus;
        }


        // Note: all methods below are copied directly from the Smack PacketReader class
        // and represent all methods that are needed for presence packet parsing.
        // Unfortunately, there is no elegant way to pass of presence packet parsing to
        // Smack core when the presence packet context is a non-standard one such as in
        // the agent-status protocol. Future Smack changes may change this situation,
        // which would allow us to delete the code copy.

        /**
         * Parses a presence packet.
         *
         * @param parser the XML parser, positioned at the start of a presence packet.
         * @return an Presence object.
         * @throws Exception if an exception occurs while parsing the packet.
         */
        private Presence parsePresence(XmlPullParser parser) throws Exception {
            Presence.Type type = Presence.Type.fromString(parser.getAttributeValue("", "type"));

            Presence presence = new Presence(type);
            presence.setTo(parser.getAttributeValue("", "to"));
            presence.setFrom(parser.getAttributeValue("", "from"));
            presence.setPacketID(parser.getAttributeValue("", "id"));

            // Parse sub-elements
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    if (elementName.equals("status")) {
                        presence.setStatus(parser.nextText());
                    }
                    else if (elementName.equals("priority")) {
                        try {
                            int priority = Integer.parseInt(parser.nextText());
                            presence.setPriority(priority);
                        }
                        catch (NumberFormatException nfe) { }
                    }
                    else if (elementName.equals("show")) {
                        presence.setMode(Presence.Mode.fromString(parser.nextText()));
                    }
                    else if (elementName.equals("error")) {
                        presence.setError(parseError(parser));
                    }
                    // Otherwise, it must be a packet extension.
                    else {
                        presence.addExtension(parsePacketExtension(elementName, namespace, parser));
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("presence")) {
                        done = true;
                    }
                }
            }
            return presence;
        }

        /**
         * Parses a packet extension sub-packet.
         *
         * @param elementName the XML element name of the packet extension.
         * @param namespace the XML namespace of the packet extension.
         * @param parser the XML parser, positioned at the starting element of the extension.
         * @return a PacketExtension.
         * @throws Exception if a parsing error occurs.
         */
        private PacketExtension parsePacketExtension(String elementName, String namespace,
                XmlPullParser parser) throws Exception
        {
            // See if a provider is registered to handle the extension.
            Object provider = ProviderManager.getExtensionProvider(elementName, namespace);
            if (provider != null) {
                if (provider instanceof PacketExtensionProvider) {
                    return ((PacketExtensionProvider)provider).parseExtension(parser);
                }
                else if (provider instanceof Class) {
                    return (PacketExtension)parseWithIntrospection(
                            elementName, (Class)provider, parser);
                }
            }
            // No providers registered, so use a default extension.
            DefaultPacketExtension extension = new DefaultPacketExtension(elementName, namespace);
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    // If an empty element, set the value with the empty string.
                    if (parser.isEmptyElementTag()) {
                        extension.setValue(name,"");
                    }
                    // Otherwise, get the the element text.
                    else {
                        eventType = parser.next();
                        if (eventType == XmlPullParser.TEXT) {
                            String value = parser.getText();
                            extension.setValue(name, value);
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(elementName)) {
                        done = true;
                    }
                }
            }
            return extension;
        }

        private Object parseWithIntrospection(String elementName,
                Class objectClass, XmlPullParser parser) throws Exception
        {
            boolean done = false;
            Object object = objectClass.newInstance();
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    String stringValue = parser.nextText();
                    PropertyDescriptor descriptor = new PropertyDescriptor(name, objectClass);
                    // Load the class type of the property.
                    Class propertyType = descriptor.getPropertyType();
                    // Get the value of the property by converting it from a
                    // String to the correct object type.
                    Object value = decode(propertyType, stringValue);
                    // Set the value of the bean.
                    descriptor.getWriteMethod().invoke(object, new Object[] { value });
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals(elementName)) {
                        done = true;
                    }
                }
            }
            return object;
        }

        /**
         * Decodes a String into an object of the specified type. If the object
         * type is not supported, null will be returned.
         *
         * @param type the type of the property.
         * @param value the encode String value to decode.
         * @return the String value decoded into the specified type.
         */
        private static Object decode(Class type, String value) throws Exception {
            if (type.getName().equals("java.lang.String")) {
                return value;
            }
            if (type.getName().equals("boolean")) {
                return Boolean.valueOf(value);
            }
            if (type.getName().equals("int")) {
                return Integer.valueOf(value);
            }
            if (type.getName().equals("long")) {
                return Long.valueOf(value);
            }
            if (type.getName().equals("float")) {
                return Float.valueOf(value);
            }
            if (type.getName().equals("double")) {
                return Double.valueOf(value);
            }
            if (type.getName().equals("java.lang.Class")) {
                return Class.forName(value);
            }
            return null;
        }

        /**
         * Parses error sub-packets.
         *
         * @param parser the XML parser.
         * @return an error sub-packet.
         * @throws Exception if an exception occurs while parsing the packet.
         */
        private XMPPError parseError(XmlPullParser parser) throws Exception {
            String errorCode = null;
            for (int i=0; i<parser.getAttributeCount(); i++) {
                if (parser.getAttributeName(i).equals("code")) {
                    errorCode = parser.getAttributeValue("", "code");
                }
            }
            String message = parser.nextText();
            while (true) {
                if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals("error")) {
                    break;
                }
            }
            return new XMPPError(Integer.parseInt(errorCode), message);
        }
    }
}