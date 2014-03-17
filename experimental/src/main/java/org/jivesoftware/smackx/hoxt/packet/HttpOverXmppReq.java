/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.packet;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.hoxt.HOXTManager;

/**
 * Represents Req IQ packet.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppReq extends AbstractHttpOverXmpp {

    private Req req;

    @Override
    public String getChildElementXML() {
        return req.toXML();
    }

    /**
     * Returns Req element.
     *
     * @return Req element
     */
    public Req getReq() {
        return req;
    }

    /**
     * Sets Req element.
     *
     * @param req Req element
     */
    public void setReq(Req req) {
        this.req = req;
    }

    /**
     * Represents Req element.
     */
    public static class Req extends AbstractBody {

        private HttpMethod method;
        private String resource;

        // TODO: validate:  xs:minInclusive value='256' xs:maxInclusive value='65536'
        private int maxChunkSize = 0; // 0 means not set

        private boolean sipub = true;

        private boolean ibb = true;
        private boolean jingle = true;

        /**
         * Creates this object.
         *
         * @param method   method attribute
         * @param resource resource attribute
         */
        public Req(HttpMethod method, String resource) {
            this.method = method;
            this.resource = resource;
        }

        @Override
        protected String getStartTag() {
            StringBuilder builder = new StringBuilder();
            builder.append("<req");
            builder.append(" ");
            builder.append("xmlns='").append(HOXTManager.NAMESPACE).append("'");
            builder.append(" ");
            builder.append("method='").append(method.toString()).append("'");
            builder.append(" ");
            builder.append("resource='").append(StringUtils.escapeForXML(resource)).append("'");
            builder.append(" ");
            builder.append("version='").append(StringUtils.escapeForXML(version)).append("'");
            if (maxChunkSize != 0) {
                builder.append(" ");
                builder.append("maxChunkSize='").append(Integer.toString(maxChunkSize)).append("'");
            }
            builder.append(" ");
            builder.append("sipub='").append(Boolean.toString(sipub)).append("'");
            builder.append(" ");
            builder.append("ibb='").append(Boolean.toString(ibb)).append("'");
            builder.append(" ");
            builder.append("jingle='").append(Boolean.toString(jingle)).append("'");
            builder.append(">");
            return builder.toString();
        }

        @Override
        protected String getEndTag() {
            return "</req>";
        }

        /**
         * Returns method attribute.
         *
         * @return method attribute
         */
        public HttpMethod getMethod() {
            return method;
        }

        /**
         * Returns resource attribute.
         *
         * @return resource attribute
         */
        public String getResource() {
            return resource;
        }

        /**
         * Returns maxChunkSize attribute.
         *
         * @return maxChunkSize attribute
         */
        public int getMaxChunkSize() {
            return maxChunkSize;
        }

        /**
         * Sets maxChunkSize attribute.
         *
         * @param maxChunkSize maxChunkSize attribute
         */
        public void setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
        }

        /**
         * Returns sipub attribute.
         *
         * @return sipub attribute
         */
        public boolean isSipub() {
            return sipub;
        }

        /**
         * Sets sipub attribute.
         *
         * @param sipub sipub attribute
         */
        public void setSipub(boolean sipub) {
            this.sipub = sipub;
        }

        /**
         * Returns ibb attribute.
         *
         * @return ibb attribute
         */
        public boolean isIbb() {
            return ibb;
        }

        /**
         * Sets ibb attribute.
         *
         * @param ibb ibb attribute
         */
        public void setIbb(boolean ibb) {
            this.ibb = ibb;
        }

        /**
         * Returns jingle attribute.
         *
         * @return jingle attribute
         */
        public boolean isJingle() {
            return jingle;
        }

        /**
         * Sets jingle attribute.
         *
         * @param jingle jingle attribute
         */
        public void setJingle(boolean jingle) {
            this.jingle = jingle;
        }
    }
}
