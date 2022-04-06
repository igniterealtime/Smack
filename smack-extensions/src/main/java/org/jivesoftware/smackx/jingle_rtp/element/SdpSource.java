/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.element;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * Implements <code>AbstractExtensionElement</code> for the <code>source</code> element defined below.
 * @see <a href="https://xmpp.org/extensions/xep-0339.html">XEP-0339: Source-Specific Media Attributes in Jingle 1.0.1 (2021-10-23)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5576">Source-Specific Media Attributes in the Session Description Protocol (SDP)</a>
 *
 * @author Lyubomir Marinov
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class SdpSource extends AbstractXmlElement {
    /**
     * The XML name of the <code>setup</code> element defined by Source-Specific Media Attributes in Jingle.
     */
    public static final String ELEMENT = "source";

    /**
     * The XML namespace of the <code>setup</code> element defined by Source-Specific Media Attributes in Jingle.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:ssma:0";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The XML name of the attribute which corresponds to the <code>rid</code> attribute in SDP.
     */
    public static final String ATTR_RID = "rid";

    /**
     * The XML name of the <code>setup</code> element's attribute which corresponds to the <code>ssrc</code>
     * media attribute in SDP.
     */
    public static final String ATTR_SSRC = "ssrc";

    /**
     * <code>SdpSource</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public SdpSource() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>SdpSource</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpSource(Builder builder) {
        super(builder);
    }

    /**
     * Gets the parameters (as defined by Source-Specific Media Attributes in Jingle) of this source.
     *
     * @return the <code>ParameterExtensionElement</code>s of this source
     */
    public List<ParameterElement> getParameters() {
        return getChildElements(ParameterElement.class);
    }

    /**
     * Finds the value of SSRC parameter identified by given name.
     *
     * @param name the name of SSRC parameter to find.
     * @return value of SSRC parameter
     */
    public String getParameter(String name) {
        for (ParameterElement param : getParameters()) {
            if (name.equals(param.getName()))
                return param.getValue();
        }
        return null;
    }

    /**
     * Gets the synchronization source (SSRC) ID of this source.
     *
     * @return the synchronization source (SSRC) ID of this source
     */
    public long getSSRC() {
        String s = getAttributeValue(ATTR_SSRC);

        return (s == null) ? -1 : Long.parseLong(s);
    }

    /**
     * Check if this source has an ssrc.
     *
     * @return true if it has an ssrc, false otherwise
     */
    public boolean hasSSRC() {
        return getAttributeValue(ATTR_SSRC) != null;
    }

    /**
     * Gets the rid of this source, if it has one.
     *
     * @return the rid of the source or null
     */
    public String getRid() {
        return getAttributeValue(ATTR_RID);
    }

    /**
     * Check if this source has an rid.
     *
     * @return true if it has an rid, false otherwise
     */
    public boolean hasRid() {
        return getAttributeValue(ATTR_RID) != null;
    }

    /**
     * Check if this source matches the given one with regards to
     * matching source identifiers (ssrc or rid).
     *
     * @param other the other SdpSourceGroup to compare to
     * @return true if this SdpSourceGroup and the one
     * given have matching source identifiers.  NOTE: will return
     * false if neither SdpSourceGroup has any source identifier set
     */
    public boolean sourceEquals(SdpSource other) {
        if (hasSSRC() && other.hasSSRC()) {
            return getSSRC() == other.getSSRC();
        } else if (hasRid() && other.hasRid()) {
            return getRid().equals(other.getRid());
        }
        return false;
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpSource. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpSource.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, SdpSource> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Adds a specific parameter (as defined by Source-Specific Media Attributes in Jingle) to this source.
         *
         * @param parameter the <code>ParameterElement</code> to add to this source
         * @return builder instance
         */
        public Builder addParameter(ParameterElement parameter) {
            addChildElement(parameter);
            return this;
        }

        /**
         * Sets the synchronization source (SSRC) ID of this source.
         *
         * @param ssrc the synchronization source (SSRC) ID to be set on this source
         * @return builder instance
         */
        public Builder setSsrc(long ssrc) {
            if (ssrc == -1) {
                removeAttribute(ATTR_SSRC);
            } else {
                addAttribute(ATTR_SSRC, Long.toString(0xffffffffL & ssrc));
            }
            return this;
        }

        /**
         * Sets the rid of this source.
         *
         * @param rid the rid to be set (or null to clear the existing rid)
         * @return builder instance
         */
        public Builder setRid(String rid) {
            if (rid == null) {
                removeAttribute(ATTR_RID);
            } else {
                addAttribute(ATTR_RID, rid);
            }
            return this;
        }

        @Override
        public SdpSource build() {
            return new SdpSource(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
