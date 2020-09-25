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
package org.jivesoftware.smackx.caps2;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public final class Caps2Manager extends Manager {

    public static Map<XMPPConnection, Caps2Manager> INSTANCES = new HashMap<>();
    private static String defaultAlgo = "sha-256";

    private static boolean autoEnable = true;

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    public static synchronized Caps2Manager getInstanceFor(XMPPConnection connection) {
        Caps2Manager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new Caps2Manager(connection);
        }
        return manager;
    }

    private Caps2Manager(XMPPConnection connection) {
        super(connection);
        INSTANCES.put(connection, this);

        if (autoEnable) {
            publishSupportForECaps2();
        }
    }

    public void publishSupportForECaps2() {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        sdm.addFeature(Caps2Element.NAMESPACE);
    }

    public void publishEntityCapabilities() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, UnsupportedEncodingException, NoSuchAlgorithmException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        DiscoverInfo discoverInfo = sdm.discoverInfo(connection().getUser());

        List<String> algoList = Collections.singletonList(defaultAlgo);

        Caps2Element element = generateCapabilityHash(discoverInfo, algoList);

        Presence presence = PresenceBuilder
                                        .buildPresence()
                                        .addExtension(element)
                                        .build();
        connection().sendStanza(presence);
    }

    public static Caps2Element generateCapabilityHash(DiscoverInfo di, List<String> algoList) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        if (algoList.size() == 0) {
            algoList.add(defaultAlgo);
        }

        // Step 1 : Incase of presence of an element other than <identity>, <feature> or ServiceDiscovery Extensions, throw error.

        // Step 2 : If <x> contains a <reported> or <item> element, abort with an error.
        DataForm extendedInfo = DataForm.from(di);
        if (extendedInfo != null) {
            if (extendedInfo.getItems().size() != 0 || extendedInfo.getReportedData() != null) {
                throw new IllegalArgumentException(" <x> should not contain a <reported> or <item> element");
            }
        }

        // Step 3 : If <x> does not adhere to "FORM_TYPE" protocol from XEP-0068, abort with an error.

        // Step 4 : Process <feature> elements.
        List<DiscoverInfo.Feature> features = di.getFeatures();

        SortedSet<String> featureSortedSet = new TreeSet<>();

        for (DiscoverInfo.Feature feature : features) {
            featureSortedSet.add(feature.getVar());
        }

        String featureString = "";
        Iterator<String> iterator = featureSortedSet.iterator();
        while (iterator.hasNext()) {
            featureString += getHexString(iterator.next());
            featureString += "1f";
        }
        featureString += "1c";

        // Step 5 : Process <identity> elements.
        List<DiscoverInfo.Identity> identities = di.getIdentities();

        SortedSet<String> identitySortedSet = new TreeSet<>();

        for (DiscoverInfo.Identity identity : identities) {
            identitySortedSet.add(getHexString(identity.getCategory()) + "1f"
                    + getHexString(identity.getType()) + "1f"
                    + getHexString(identity.getLanguage()) + "1f"
                    + getHexString(identity.getName()) + "1f"
                    + "1e");
        }

        String identityString = "";
        Iterator<String> iterator1 = identitySortedSet.iterator();
        while (iterator1.hasNext()) {
            identityString += iterator1.next();
        }
        identityString += "1c";

        // Step 6 : Processing of Service Discovery Extensions.
        // @TODO : Add support for multiple service discovery extensions.

        String extensionString = "";

        if (extendedInfo != null) {
            List<FormField> fields = extendedInfo.getFields();
            Iterator<FormField> formFieldIterator = fields.iterator();

            SortedSet<String> extendedSortedSet = new TreeSet<>();

            while (formFieldIterator.hasNext()) {
                FormField formField = formFieldIterator.next();

                String valuesInField = "";
                SortedSet<String> valueSortedSet = new TreeSet<>();
                List<String> valueStringList = formField.getValuesAsString();
                Iterator<String> valueListIterator = valueStringList.iterator();
                while (valueListIterator.hasNext()) {
                    valueSortedSet.add(getHexString(valueListIterator.next()) + "1f");
                }
                Iterator<String> iterator2 = valueSortedSet.iterator();
                while (iterator2.hasNext()) {
                    valuesInField += iterator2.next();
                }
                valuesInField = getHexString(formField.getFieldName()) + "1f" + valuesInField;
                valuesInField += "1e";
                extendedSortedSet.add(valuesInField);
            }

            Iterator<String> extendedSortedSetIterator = extendedSortedSet.iterator();
            while (extendedSortedSetIterator.hasNext()) {
                extensionString += extendedSortedSetIterator.next();
            }
            extensionString += "1d";
        }
        extensionString += "1c";
        String finalHexString =  featureString + identityString + extensionString;

        byte[] input = hexStringToByteArray(finalHexString);

        Caps2Element element = HashFunctions.digest(input, algoList);

        return element;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String getHexString(String attribute) throws UnsupportedEncodingException {
        String str = attribute;
        StringBuffer sb = new StringBuffer();
        if (str != null) {

            byte[] ch = str.getBytes("UTF-8");
            String hexString;

            for (int i = 0; i < ch.length; i++) {
                if (ch[i] < 0) {
                    hexString = Integer.toHexString(ch[i]);
                    int lastIndexOf_d = hexString.lastIndexOf("f");
                    hexString = hexString.substring(lastIndexOf_d + 1);
                }
                else {
                    hexString = Integer.toHexString(ch[i]);
                }
                sb.append(hexString);
            }
        }
        return sb.toString();
    }
}
