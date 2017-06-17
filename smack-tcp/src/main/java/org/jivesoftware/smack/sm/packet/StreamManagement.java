/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sm.packet;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.StanzaErrorTextElement;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class StreamManagement {
    public static final String NAMESPACE = "urn:xmpp:sm:3";

    public static final class StreamManagementFeature implements ExtensionElement {

        public static final String ELEMENT = "sm";
        public static final StreamManagementFeature INSTANCE = new StreamManagementFeature();

        private StreamManagementFeature() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.closeEmptyElement();
            return xml;
        }
    }

    private static abstract class AbstractEnable implements Nonza {

        /**
         * Preferred maximum resumption time in seconds (optional).
         */
        protected int max = -1;

        protected boolean resume = false;

        protected void maybeAddResumeAttributeTo(XmlStringBuilder xml) {
            if (resume) {
                // XEP 198 never mentions the case where resume='false', it's either set to true or
                // not set at all. We reflect this in this code part
                xml.attribute("resume", "true");
            }
        }

        protected void maybeAddMaxAttributeTo(XmlStringBuilder xml) {
            if (max > 0) {
                xml.attribute("max", Integer.toString(max));
            }
        }

        public boolean isResumeSet() {
            return resume;
        }

        /**
         * Return the max resumption time in seconds.
         * @return the max resumption time in seconds
         */
        public int getMaxResumptionTime() {
            return max;
        }

        @Override
        public final String getNamespace() {
            return NAMESPACE;
        }
    }

    public static class Enable extends AbstractEnable {
        public static final String ELEMENT = "enable";

        public static final Enable INSTANCE = new Enable();

        private Enable() {
        }

        public Enable(boolean resume) {
            this.resume = resume;
        }

        public Enable(boolean resume, int max) {
            this(resume);
            this.max = max;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            maybeAddResumeAttributeTo(xml);
            maybeAddMaxAttributeTo(xml);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * A Stream Management 'enabled' element.
     * <p>
     * Here is a full example, all attributes besides 'xmlns' are optional.
     * </p>
     * <pre>
     * {@code
     * <enabled xmlns='urn:xmpp:sm:3'
     *      id='some-long-sm-id'
     *      location='[2001:41D0:1:A49b::1]:9222'
     *      resume='true'/>
     * }
     * </pre>
     */
    public static class Enabled extends AbstractEnable {
        public static final String ELEMENT = "enabled";

        /**
         * The stream id ("SM-ID")
         */
        private final String id;

        /**
         * The location where the server prefers reconnection.
         */
        private final String location;

        public Enabled(String id, boolean resume) {
            this(id, resume, null, -1);
        }

        public Enabled(String id, boolean resume, String location, int max) {
            this.id = id;
            this.resume = resume;
            this.location = location;
            this.max = max;
        }

        public String getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("id", id);
            maybeAddResumeAttributeTo(xml);
            xml.optAttribute("location", location);
            maybeAddMaxAttributeTo(xml);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static class Failed implements Nonza {
        public static final String ELEMENT = "failed";

        private final XMPPError.Condition condition;

        private final List<StanzaErrorTextElement> textElements;


        public Failed() {
            this(null, null);
        }

        public Failed(XMPPError.Condition condition, List<StanzaErrorTextElement> textElements) {
            this.condition = condition;
            if (textElements == null) {
                this.textElements = Collections.emptyList();
            } else {
                this.textElements = Collections.unmodifiableList(textElements);
            }
        }

        public XMPPError.Condition getXMPPErrorCondition() {
            return condition;
        }

        public List<StanzaErrorTextElement> getTextElements() {
            return textElements;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            if (condition == null && textElements.isEmpty()) {
                xml.closeEmptyElement();
            } else {
                xml.rightAngleBracket();
                if (condition != null) {
                    // TODO This should use StanzaError (formerly XMPPError) in Smack 4.3 (see SMACK-769)
                    xml.append(condition.toString());
                    xml.xmlnsAttribute(XMPPError.NAMESPACE);
                }
                xml.append(textElements);
                xml.closeElement(ELEMENT);
            }
            return xml;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

    }

    private static abstract class AbstractResume implements Nonza {

        private final long handledCount;
        private final String previd;

        public AbstractResume(long handledCount, String previd) {
            this.handledCount = handledCount;
            this.previd = previd;
        }

        public long getHandledCount() {
            return handledCount;
        }

        public String getPrevId() {
            return previd;
        }

        @Override
        public final String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public final XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("h", Long.toString(handledCount));
            xml.attribute("previd", previd);
            xml.closeEmptyElement();
            return xml;
        }
    }

    public static class Resume extends AbstractResume {
        public static final String ELEMENT = "resume";

        public Resume(long handledCount, String previd) {
            super(handledCount, previd);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static class Resumed extends AbstractResume {
        public static final String ELEMENT = "resumed";

        public Resumed(long handledCount, String previd) {
            super(handledCount, previd);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static class AckAnswer implements Nonza {
        public static final String ELEMENT = "a";

        private final long handledCount;

        public AckAnswer(long handledCount) {
            this.handledCount = handledCount;
        }

        public long getHandledCount() {
            return handledCount;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("h", Long.toString(handledCount));
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static final class AckRequest implements Nonza {
        public static final String ELEMENT = "r";
        public static final AckRequest INSTANCE = new AckRequest();

        private AckRequest() {
        }

        @Override
        public CharSequence toXML() {
            return '<' + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
