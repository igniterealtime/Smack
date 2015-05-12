/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChatException.MucConfigurationNotSupportedException;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.util.JidUtil;

/**
 * Multi-User Chat configuration form manager is used to fill out and submit a {@link Form} used to
 * configure rooms.
 * <p>
 * Room configuration needs either be done right after the room is created and still locked. Or at
 * any later point (see <a href="http://xmpp.org/extensions/xep-0045.html#roomconfig">XEP-45 § 10.2
 * Subsequent Room Configuration</a>). When done with the configuration, call
 * {@link #submitConfigurationForm()}.
 * </p>
 * <p>
 * The manager may not provide all possible configuration options. If you want direct access to the
 * configuraiton form, use {@link MultiUserChat#getConfigurationForm()} and
 * {@link MultiUserChat#sendConfigurationForm(Form)}.
 * </p>
 */
public class MucConfigFormManager {
    public static final String MUC_ROOMCONFIG_ROOMOWNERS = "muc#roomconfig_roomowners";

    private final MultiUserChat multiUserChat;
    private final Form answerForm;
    private final List<Jid> owners;

    /**
     * Create a new MUC config form manager.
     * <p>
     * Note that the answerForm needs to be filled out with the defaults.
     * </p>
     *
     * @param multiUserChat the MUC for this configuration form.
     * @throws InterruptedException 
     * @throws NotConnectedException 
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     */
    MucConfigFormManager(MultiUserChat multiUserChat) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        this.multiUserChat = multiUserChat;

        // Set the answer form
        Form configForm = multiUserChat.getConfigurationForm();
        this.answerForm = configForm.createAnswerForm();
        // Add the default answers to the form to submit
        for (FormField field : configForm.getFields()) {
            if (field.getType() == FormField.Type.hidden
                            || StringUtils.isNullOrEmpty(field.getVariable())) {
                continue;
            }
            answerForm.setDefaultAnswer(field.getVariable());
        }

        // Set the local variables according to the fields found in the answer form
        if (answerForm.hasField(MUC_ROOMCONFIG_ROOMOWNERS)) {
            // Set 'owners' to the currently configured owners
            List<String> ownerStrings = answerForm.getField(MUC_ROOMCONFIG_ROOMOWNERS).getValues();
            owners = new ArrayList<>(ownerStrings.size());
            JidUtil.jidsFrom(ownerStrings, owners, null);
        }
        else {
            // roomowners not supported, this should barely be the case
            owners = null;
        }
    }

    public boolean supportsRoomOwners() {
        return owners != null;
    }

    public MucConfigFormManager setRoomOwners(Collection<? extends Jid> newOwners) throws MucConfigurationNotSupportedException {
        if (!supportsRoomOwners()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ROOMOWNERS);
        }
        owners.clear();
        owners.addAll(newOwners);
        return this;
    }

    public void submitConfigurationForm() throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        if (owners != null) {
            answerForm.setAnswer(MUC_ROOMCONFIG_ROOMOWNERS, JidUtil.toStringList(owners));
        }
        multiUserChat.sendConfigurationForm(answerForm);
    }
}
