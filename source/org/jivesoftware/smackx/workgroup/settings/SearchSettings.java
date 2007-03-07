/**
 * $RCSfile$
 * $Revision: 38648 $
 * $Date: 2006-12-27 01:46:18 -0800 (Wed, 27 Dec 2006) $
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is
 subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.settings;

import org.jivesoftware.smackx.workgroup.util.ModelUtil;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class SearchSettings extends IQ {
    private String forumsLocation;
    private String kbLocation;

    public boolean isSearchEnabled() {
        return ModelUtil.hasLength(getForumsLocation()) && ModelUtil.hasLength(getKbLocation());
    }

    public String getForumsLocation() {
        return forumsLocation;
    }

    public void setForumsLocation(String forumsLocation) {
        this.forumsLocation = forumsLocation;
    }

    public String getKbLocation() {
        return kbLocation;
    }

    public void setKbLocation(String kbLocation) {
        this.kbLocation = kbLocation;
    }

    public boolean hasKB(){
        return ModelUtil.hasLength(getKbLocation());
    }

    public boolean hasForums(){
        return ModelUtil.hasLength(getForumsLocation());
    }


    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "search-settings";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=");
        buf.append('"');
        buf.append(NAMESPACE);
        buf.append('"');
        buf.append("></").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }


    /**
     * Packet extension provider for AgentStatusRequest packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            SearchSettings settings = new SearchSettings();

            boolean done = false;
            String kb = null;
            String forums = null;

            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("forums".equals(parser.getName()))) {
                    forums = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("kb".equals(parser.getName()))) {
                    kb = parser.nextText();
                }
                else if (eventType == XmlPullParser.END_TAG && "search-settings".equals(parser.getName())) {
                    done = true;
                }
            }

            settings.setForumsLocation(forums);
            settings.setKbLocation(kb);
            return settings;
        }
    }
}
