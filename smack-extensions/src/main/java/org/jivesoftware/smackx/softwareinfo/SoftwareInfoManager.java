/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.softwareinfo;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.FeatureNotSupportedException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.softwareinfo.form.SoftwareInfoForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
* Entry point for Smack's API for XEP-0232: Software Information.
* <br>
* @see <a href="https://xmpp.org/extensions/xep-0232.html">
*     XEP-0232 : Software Information.
*     </a>
*/
public final class SoftwareInfoManager extends Manager {

    private static final String FEATURE = "http://jabber.org/protocol/disco";
    private static final Map<XMPPConnection, SoftwareInfoManager> INSTANCES = new WeakHashMap<>();
    private final ServiceDiscoveryManager serviceDiscoveryManager;

    public static synchronized SoftwareInfoManager getInstanceFor (XMPPConnection connection) {
        SoftwareInfoManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new SoftwareInfoManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private SoftwareInfoManager(XMPPConnection connection) {
        super(connection);
        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
    }

    /**
     * Returns true if the feature is supported by the Jid.
     * <br>
     * @param jid Jid to be checked for support
     * @return boolean
     * @throws NoResponseException if there was no response from the remote entity
     * @throws XMPPErrorException if there was an XMPP error returned
     * @throws NotConnectedException if the XMPP connection is not connected
     * @throws InterruptedException if the calling thread was interrupted
     */
    public boolean isSupported(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return serviceDiscoveryManager.supportsFeatures(jid, FEATURE);
    }

    /**
     * Publishes the provided {@link SoftwareInfoForm} as an extended info.
     * <br>
     * @param softwareInfoForm form to be added as an extended info
     */
    public void publishSoftwareInformationForm(SoftwareInfoForm softwareInfoForm) {
        serviceDiscoveryManager.addExtendedInfo(softwareInfoForm.getDataForm());
    }

    /**
     * Get SoftwareInfoForm from Jid provided.
     * <br>
     * @param jid jid to get software information from
     * @return {@link SoftwareInfoForm} Form containing software information
     * @throws NoResponseException if there was no response from the remote entity
     * @throws XMPPErrorException if there was an XMPP error returned
     * @throws NotConnectedException if the XMPP connection is not connected
     * @throws InterruptedException if the calling thread was interrupted
     * @throws FeatureNotSupportedException if the feature is not supported
     */
    public SoftwareInfoForm fromJid(Jid jid) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, FeatureNotSupportedException {
        if (!isSupported(jid)) {
            throw new FeatureNotSupportedException(SoftwareInfoForm.FORM_TYPE, jid);
        }
        DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(jid);
        DataForm dataForm = DataForm.from(discoverInfo, SoftwareInfoForm.FORM_TYPE);
        if (dataForm == null) {
            return null;
        }
        return SoftwareInfoForm.getBuilder()
                               .setDataForm(dataForm)
                               .build();
    }
}
