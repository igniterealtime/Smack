/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.packet;

import java.util.Locale;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The base IQ (Info/Query) packet. IQ packets are used to get and set information
 * on the server, including authentication, roster operations, and creating
 * accounts. Each IQ packet has a specific type that indicates what type of action
 * is being taken: "get", "set", "result", or "error".<p>
 *
 * IQ packets can contain a single child element that exists in a specific XML
 * namespace. The combination of the element name and namespace determines what
 * type of IQ packet it is. Some example IQ subpacket snippets:<ul>
 *
 *  <li>&lt;query xmlns="jabber:iq:auth"&gt; -- an authentication IQ.
 *  <li>&lt;query xmlns="jabber:iq:private"&gt; -- a private storage IQ.
 *  <li>&lt;pubsub xmlns="http://jabber.org/protocol/pubsub"&gt; -- a pubsub IQ.
 * </ul>
 *
 * @author Matt Tucker
 */
public abstract class IQ extends Packet {

    public static final String QUERY_ELEMENT = "query";

    private Type type = Type.get;

    public IQ() {
        super();
    }

    public IQ(IQ iq) {
        super(iq);
        type = iq.getType();
    }
    /**
     * Returns the type of the IQ packet.
     *
     * @return the type of the IQ packet.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the IQ packet.
     *
     * @param type the type of the IQ packet.
     */
    public void setType(Type type) {
        if (type == null) {
            this.type = Type.get;
        }
        else {
            this.type = type;
        }
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder buf = new XmlStringBuilder();
        buf.halfOpenElement("iq");
        addCommonAttributes(buf);
        if (type == null) {
            buf.attribute("type", "get");
        }
        else {
            buf.attribute("type", type.toString());
        }
        buf.rightAngleBracket();
        // Add the query section if there is one.
        buf.optAppend(getChildElementXML());
        // Add the error sub-packet, if there is one.
        XMPPError error = getError();
        if (error != null) {
            buf.append(error.toXML());
        }
        buf.closeElement("iq");
        return buf;
    }

    /**
     * Returns the sub-element XML section of the IQ packet, or <tt>null</tt> if there
     * isn't one. Packet extensions <b>must</b> be included, if any are defined.<p>
     *
     * Extensions of this class must override this method.
     *
     * @return the child element section of the IQ XML.
     */
    public abstract CharSequence getChildElementXML();

    /**
     * Convenience method to create a new empty {@link Type#result IQ.Type.result}
     * IQ based on a {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}
     * IQ. The new packet will be initialized with:<ul>
     *      <li>The sender set to the recipient of the originating IQ.
     *      <li>The recipient set to the sender of the originating IQ.
     *      <li>The type set to {@link Type#result IQ.Type.result}.
     *      <li>The id set to the id of the originating IQ.
     *      <li>No child element of the IQ element.
     * </ul>
     *
     * @param request the {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set} IQ packet.
     * @throws IllegalArgumentException if the IQ packet does not have a type of
     *      {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}.
     * @return a new {@link Type#result IQ.Type.result} IQ based on the originating IQ.
     */
    public static IQ createResultIQ(final IQ request) {
        if (!(request.getType() == Type.get || request.getType() == Type.set)) {
            throw new IllegalArgumentException(
                    "IQ must be of type 'set' or 'get'. Original IQ: " + request.toXML());
        }
        final IQ result = new IQ() {
            public String getChildElementXML() {
                return null;
            }
        };
        result.setType(Type.result);
        result.setPacketID(request.getPacketID());
        result.setFrom(request.getTo());
        result.setTo(request.getFrom());
        return result;
    }

    /**
     * Convenience method to create a new {@link Type#error IQ.Type.error} IQ
     * based on a {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}
     * IQ. The new packet will be initialized with:<ul>
     *      <li>The sender set to the recipient of the originating IQ.
     *      <li>The recipient set to the sender of the originating IQ.
     *      <li>The type set to {@link Type#error IQ.Type.error}.
     *      <li>The id set to the id of the originating IQ.
     *      <li>The child element contained in the associated originating IQ.
     *      <li>The provided {@link XMPPError XMPPError}.
     * </ul>
     *
     * @param request the {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set} IQ packet.
     * @param error the error to associate with the created IQ packet.
     * @throws IllegalArgumentException if the IQ packet does not have a type of
     *      {@link Type#get IQ.Type.get} or {@link Type#set IQ.Type.set}.
     * @return a new {@link Type#error IQ.Type.error} IQ based on the originating IQ.
     */
    public static IQ createErrorResponse(final IQ request, final XMPPError error) {
        if (!(request.getType() == Type.get || request.getType() == Type.set)) {
            throw new IllegalArgumentException(
                    "IQ must be of type 'set' or 'get'. Original IQ: " + request.toXML());
        }
        final IQ result = new IQ() {
            @Override
            public CharSequence getChildElementXML() {
                return request.getChildElementXML();
            }
        };
        result.setType(Type.error);
        result.setPacketID(request.getPacketID());
        result.setFrom(request.getTo());
        result.setTo(request.getFrom());
        result.setError(error);
        return result;
    }

    /**
     * A enum to represent the type of the IQ packet. The types are:
     *
     * <ul>
     *      <li>IQ.Type.get
     *      <li>IQ.Type.set
     *      <li>IQ.Type.result
     *      <li>IQ.Type.error
     * </ul>
     */
    public enum Type {

        get,
        set,
        result,
        error;

        /**
         * Converts a String into the corresponding types. Valid String values
         * that can be converted to types are: "get", "set", "result", and "error".
         *
         * @param string the String value to covert.
         * @return the corresponding Type.
         * @throws IllegalArgumentException when not able to parse the string parameter
         * @throws NullPointerException if the string is null
         */
        public static Type fromString(String string) {
            return Type.valueOf(string.toLowerCase(Locale.US));
        }
    }
}
