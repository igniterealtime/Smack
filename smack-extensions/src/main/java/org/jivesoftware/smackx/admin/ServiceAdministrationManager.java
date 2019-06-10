/**
 *
 * Copyright 2016-2019 Florian Schmaus
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
package org.jivesoftware.smackx.admin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.xdata.Form;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

public class ServiceAdministrationManager extends Manager {

    public static final String COMMAND_NODE = "http://jabber.org/protocol/admin";

    private static final String COMMAND_NODE_HASHSIGN = COMMAND_NODE + '#';

    private static final Map<XMPPConnection, ServiceAdministrationManager> INSTANCES = new WeakHashMap<>();

    public static synchronized ServiceAdministrationManager getInstanceFor(XMPPConnection connection) {
        ServiceAdministrationManager serviceAdministrationManager = INSTANCES.get(connection);
        if (serviceAdministrationManager == null) {
            serviceAdministrationManager = new ServiceAdministrationManager(connection);
            INSTANCES.put(connection, serviceAdministrationManager);
        }
        return serviceAdministrationManager;
    }

    private final AdHocCommandManager adHocCommandManager;

    public ServiceAdministrationManager(XMPPConnection connection) {
        super(connection);

        adHocCommandManager = AdHocCommandManager.getAddHocCommandsManager(connection);
    }

    public RemoteCommand addUser() {
        return addUser(connection().getXMPPServiceDomain());
    }

    public RemoteCommand addUser(Jid service) {
        return adHocCommandManager.getRemoteCommand(service, COMMAND_NODE_HASHSIGN + "add-user");
    }

    public void addUser(final EntityBareJid userJid, final String password)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        RemoteCommand command = addUser();
        command.execute();

        Form answerForm = command.getForm().createAnswerForm();

        answerForm.setAnswer("accountjid", userJid);
        answerForm.setAnswer("password", password);
        answerForm.setAnswer("password-verify", password);

        command.execute(answerForm);
        assert (command.isCompleted());
    }

    public RemoteCommand deleteUser() {
        return deleteUser(connection().getXMPPServiceDomain());
    }

    public RemoteCommand deleteUser(Jid service) {
        return adHocCommandManager.getRemoteCommand(service, COMMAND_NODE_HASHSIGN + "delete-user");
    }

    public void deleteUser(EntityBareJid userJidToDelete)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Set<EntityBareJid> userJidsToDelete = Collections.singleton(userJidToDelete);
        deleteUser(userJidsToDelete);
    }

    public void deleteUser(Set<EntityBareJid> jidsToDelete)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        RemoteCommand command = deleteUser();
        command.execute();

        Form answerForm = command.getForm().createAnswerForm();

        answerForm.setAnswer("accountjids", jidsToDelete);

        command.execute(answerForm);
        assert (command.isCompleted());
    }
}
