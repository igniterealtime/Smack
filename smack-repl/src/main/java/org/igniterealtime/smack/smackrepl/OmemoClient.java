/**
 *
 * Copyright 2019 Paul Schaub
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.smackrepl;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.dns.javax.JavaxResolver;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoMessage;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;
import org.jivesoftware.smackx.omemo.listener.OmemoMucMessageListener;
import org.jivesoftware.smackx.omemo.signal.SignalCachingOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalFileBasedOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.OmemoTrustCallback;
import org.jivesoftware.smackx.omemo.trust.TrustState;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class OmemoClient {

    public static final Logger LOGGER = Logger.getLogger(OmemoClient.class.getName());

    public static void init() {
        SmackConfiguration.getVersion();
        JavaxResolver.setup();
    }

    private static final Scanner scanner = new Scanner(System.in, "UTF-8");
    private final XMPPConnection connection;
    private final OmemoManager omemoManager;

    public static void main(String[] args) {
        SmackConfiguration.DEBUG = true;
        if (args.length != 2) {
            print("Missing arguments: <jid> <password>");
            return;
        }
        SignalOmemoService.acknowledgeLicense();
        SignalOmemoService.setup();
        SignalOmemoService omemoService = (SignalOmemoService) SignalOmemoService.getInstance();
        omemoService.setOmemoStoreBackend(new SignalCachingOmemoStore(new SignalFileBasedOmemoStore(new File("omemo-store"))));

        EntityBareJid jid = JidCreate.entityBareFromOrThrowUnchecked(args[0]);
        String password = args[1];
        OmemoClient client = new OmemoClient(jid, password);
        client.start();

        while (true) {
            String input = scanner.nextLine();
            if (input.startsWith("/quit")) {
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            client.handleInput(input);
        }
    }

    public OmemoClient(EntityBareJid jid, String password) {
        connection = new XMPPTCPConnection(XMPPTCPConnectionConfiguration.builder()
                .setXmppAddressAndPassword(jid, password).build());
        connection.setReplyTimeout(10 * 1000);
        omemoManager = OmemoManager.getInstanceFor(connection);
        omemoManager.setTrustCallback(new OmemoTrustCallback() {
            // In a real app you'd want to persist these decisions
            private final Map<OmemoFingerprint, TrustState> trustStateMap = new HashMap<>();
            @Override
            public TrustState getTrust(OmemoDevice device, OmemoFingerprint fingerprint) {
                return trustStateMap.get(fingerprint) != null ? trustStateMap.get(fingerprint) : TrustState.undecided;
            }

            @Override
            public void setTrust(OmemoDevice device, OmemoFingerprint fingerprint, TrustState state) {
                trustStateMap.put(fingerprint, state);
            }
        });
        omemoManager.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(Stanza s, OmemoMessage.Received m) {
                print(m.getSenderDevice() + ": " + (m.getBody() != null ? m.getBody() : "<keyTransportMessage>"));
            }

            @Override
            public void onOmemoCarbonCopyReceived(CarbonExtension.Direction d, Message cc, Message wm, OmemoMessage.Received m) {
                onOmemoMessageReceived(cc, m);
            }
        });
        omemoManager.addOmemoMucMessageListener(new OmemoMucMessageListener() {
            @Override
            public void onOmemoMucMessageReceived(MultiUserChat muc, Stanza s, OmemoMessage.Received m) {
                print(s.getFrom() + ":" + m.getSenderDevice().getDeviceId() + ": " + (m.getBody() != null ? m.getBody() : "<keyTransportMessage>"));
            }
        });
    }

    public void start() {
        try {
            ((XMPPTCPConnection) connection).connect().login();
            omemoManager.initialize();
            print("Logged in!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected Exception", e);
        }
    }

    public void handleInput(String input) {
        String[] com = input.split(" ");
        switch (com[0]) {
            case "/omemo":
                if (com.length < 3) {
                    print("Usage: /omemo <contact-jid> <message>");
                    return;
                }
                BareJid recipient = jid(com[1]);
                if (recipient == null) return;
                StringBuilder body = new StringBuilder();
                for (int i = 2; i < com.length; i++) body.append(com[i]).append(' ');

                try {
                    connection.sendStanza(omemoManager.encrypt(recipient, body.toString()).asMessage(recipient));
                } catch (UndecidedOmemoIdentityException e) {
                    print("Undecided Identities!\n" + Arrays.toString(e.getUndecidedDevices().toArray()));
                } catch (SmackException.NotConnectedException | InterruptedException | CryptoFailedException | SmackException.NoResponseException | SmackException.NotLoggedInException e) {
                    LOGGER.log(Level.SEVERE, "Unexpected Exception", e);
                }
                break;
            case "/trust":
                print("Trust");
                if (com.length != 2) {
                    print("Usage: /trust <contact-jid>");
                }
                BareJid contact = jid(com[1]);
                if (contact == null) return;
                HashMap<OmemoDevice, OmemoFingerprint> devices;
                try {
                    devices = omemoManager.getActiveFingerprints(contact);
                } catch (SmackException.NotLoggedInException | CorruptedOmemoKeyException | CannotEstablishOmemoSessionException | SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {

                    return;
                }
                for (OmemoDevice d : devices.keySet()) {
                    print("Trust (1) or distrust (2)?\n" + devices.get(d).blocksOf8Chars());
                    if (Integer.parseInt(scanner.nextLine()) == 1) {
                        omemoManager.trustOmemoIdentity(d, devices.get(d));
                    } else {
                        omemoManager.distrustOmemoIdentity(d, devices.get(d));
                    }
                }
                print("Done.");
                break;
            case "/purge":
                try {
                    omemoManager.purgeDeviceList();
                    print("Purged.");
                } catch (SmackException.NotLoggedInException | InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Unexpected Exception", e);
                }
        }
    }

    public BareJid jid(String jid) {
        try {
            return JidCreate.bareFrom(jid);
        } catch (XmppStringprepException e) {
            print("Invalid Jid!");
            return null;
        }
    }

    private static void print(String msg) {
        // CHECKSTYLE:OFF
        System.out.println(msg);
        // CHECKSTYLE:ON
    }
}
