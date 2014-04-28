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
 * Represents Resp IQ packet.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppResp extends AbstractHttpOverXmpp {

    private Resp resp;

    @Override
    public String getChildElementXML() {
        return resp.toXML();
    }

    /**
     * Returns Resp element.
     *
     * @return Resp element
     */
    public Resp getResp() {
        return resp;
    }

    /**
     * Sets Resp element.
     *
     * @param resp Resp element
     */
    public void setResp(Resp resp) {
        this.resp = resp;
    }

    /**
     * Represents Resp element.
     */
    public static class Resp extends AbstractBody {

        private int statusCode;
        private String statusMessage = null;

        @Override
        protected String getStartTag() {
            StringBuilder builder = new StringBuilder();
            builder.append("<resp");
            builder.append(" ");
            builder.append("xmlns='").append(HOXTManager.NAMESPACE).append("'");
            builder.append(" ");
            builder.append("version='").append(StringUtils.escapeForXML(version)).append("'");
            builder.append(" ");
            builder.append("statusCode='").append(Integer.toString(statusCode)).append("'");
            if (statusMessage != null) {
                builder.append(" ");
                builder.append("statusMessage='").append(StringUtils.escapeForXML(statusMessage)).append("'");
            }
            builder.append(">");
            return builder.toString();
        }

        @Override
        protected String getEndTag() {
            return "</resp>";
        }

        /**
         * Returns statusCode attribute.
         *
         * @return statusCode attribute
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Sets statusCode attribute.
         *
         * @param statusCode statusCode attribute
         */
        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        /**
         * Returns statusMessage attribute.
         *
         * @return statusMessage attribute
         */
        public String getStatusMessage() {
            return statusMessage;
        }

        /**
         * Sets statusMessage attribute.
         *
         * @param statusMessage statusMessage attribute
         */
        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }
    }
}
