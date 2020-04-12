/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.exceptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.jxmpp.jid.BareJid;

/**
 * Exception gets thrown when we are unable to establish a session with a device for some reason.
 *
 * @author Paul Schaub
 */
public class CannotEstablishOmemoSessionException extends Exception {

    private static final long serialVersionUID = 3165844730283295249L;
    private final HashMap<BareJid, HashMap<OmemoDevice, Throwable>> failures = new HashMap<>();
    private final HashMap<BareJid, ArrayList<OmemoDevice>> successes = new HashMap<>();

    public CannotEstablishOmemoSessionException(OmemoDevice failed, Throwable reason) {
        super();
        getFailsOfContact(failed.getJid()).put(failed, reason);
    }

    public void addFailures(CannotEstablishOmemoSessionException otherFailures) {
        for (Map.Entry<BareJid, HashMap<OmemoDevice, Throwable>> entry : otherFailures.getFailures().entrySet()) {
            getFailsOfContact(entry.getKey()).putAll(entry.getValue());
        }
    }

    public void addSuccess(OmemoDevice success) {
        getSuccessesOfContact(success.getJid()).add(success);
    }

    public HashMap<BareJid, HashMap<OmemoDevice, Throwable>> getFailures() {
        return failures;
    }

    public HashMap<BareJid, ArrayList<OmemoDevice>> getSuccesses() {
        return successes;
    }

    private HashMap<OmemoDevice, Throwable> getFailsOfContact(BareJid contact) {
        HashMap<OmemoDevice, Throwable> h = failures.get(contact);
        if (h == null) {
            h = new HashMap<>();
            failures.put(contact, h);
        }
        return h;
    }

    private ArrayList<OmemoDevice> getSuccessesOfContact(BareJid contact) {
        ArrayList<OmemoDevice> suc = successes.get(contact);
        if (suc == null) {
            suc = new ArrayList<>();
            successes.put(contact, suc);
        }
        return suc;
    }

    /**
     * Return true, if there is at least one recipient, which would not be able to decipher the message on any of
     * their devices.
     *
     * @return true if the exception requires to be thrown
     */
    public boolean requiresThrowing() {
        for (Map.Entry<BareJid, HashMap<OmemoDevice, Throwable>> entry : failures.entrySet()) {
            ArrayList<OmemoDevice> suc = successes.get(entry.getKey());
            if (suc == null || suc.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
