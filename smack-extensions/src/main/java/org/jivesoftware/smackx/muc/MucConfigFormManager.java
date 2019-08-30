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
    /**
     * The constant String {@value}.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#owner">XEP-0045 § 10. Owner Use Cases</a>
     */
    public static final String MUC_ROOMCONFIG_ROOMOWNERS = "muc#roomconfig_roomowners";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_MEMBERSONLY = "muc#roomconfig_membersonly";

    /**
     * The constant String {@value}.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#enter-pw">XEP-0045 § 7.2.6 Password-Protected Rooms</a>
     */
    public static final String MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM = "muc#roomconfig_passwordprotectedroom";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_ROOMSECRET = "muc#roomconfig_roomsecret";

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
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
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
            List<CharSequence> ownerStrings = answerForm.getField(MUC_ROOMCONFIG_ROOMOWNERS).getValues();
            owners = new ArrayList<>(ownerStrings.size());
            JidUtil.jidsFrom(ownerStrings, owners, null);
        }
        else {
            // roomowners not supported, this should barely be the case
            owners = null;
        }
    }

    /**
     * Check if the room supports room owners.
     * @return <code>true</code> if supported, <code>false</code> if not.
     * @see #MUC_ROOMCONFIG_ROOMOWNERS
     */
    public boolean supportsRoomOwners() {
        return owners != null;
    }

    /**
     * Set the owners of the room.
     *
     * @param newOwners a collection of JIDs to become the new owners of the room.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the MUC service does not support this option.
     * @see #MUC_ROOMCONFIG_ROOMOWNERS
     */
    public MucConfigFormManager setRoomOwners(Collection<? extends Jid> newOwners) throws MucConfigurationNotSupportedException {
        if (!supportsRoomOwners()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ROOMOWNERS);
        }
        owners.clear();
        owners.addAll(newOwners);
        return this;
    }

    /**
     * Check if the room supports a members only configuration.
     *
     * @return <code>true</code> if supported, <code>false</code> if not.
     */
    public boolean supportsMembersOnly() {
        return answerForm.hasField(MUC_ROOMCONFIG_MEMBERSONLY);
    }

    /**
     * Make the room for members only.
     *
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager makeMembersOnly() throws MucConfigurationNotSupportedException {
        return setMembersOnly(true);
    }

    /**
     * Set if the room is members only. Rooms are not members only per default.
     *
     * @param isMembersOnly if the room should be members only.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setMembersOnly(boolean isMembersOnly) throws MucConfigurationNotSupportedException {
        if (!supportsMembersOnly()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_MEMBERSONLY);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_MEMBERSONLY, isMembersOnly);
        return this;
    }

    /**
     * Check if the room supports password protection.
     *
     * @return <code>true</code> if supported, <code>false</code> if not.
     */
    public boolean supportsPasswordProtected() {
        return answerForm.hasField(MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM);
    }

    /**
     * Set a password and make the room password protected. Users will need to supply the password
     * to join the room.
     *
     * @param password the password to set.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setAndEnablePassword(String password)
                    throws MucConfigurationNotSupportedException {
        return setIsPasswordProtected(true).setRoomSecret(password);
    }

    /**
     * Make the room password protected.
     *
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager makePasswordProtected() throws MucConfigurationNotSupportedException {
        return setIsPasswordProtected(true);
    }

    /**
     * Set if this room is password protected. Rooms are by default not password protected.
     *
     * @param isPasswordProtected TODO javadoc me please
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setIsPasswordProtected(boolean isPasswordProtected)
                    throws MucConfigurationNotSupportedException {
        if (!supportsMembersOnly()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM, isPasswordProtected);
        return this;
    }

    /**
     * Set the room secret, aka the room password. If set and enabled, the password is required to
     * join the room. Note that this does only set it by does not enable password protection. Use
     * {@link #setAndEnablePassword(String)} to set a password and make the room protected.
     *
     * @param secret the secret/password.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setRoomSecret(String secret)
                    throws MucConfigurationNotSupportedException {
        if (!answerForm.hasField(MUC_ROOMCONFIG_ROOMSECRET)) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ROOMSECRET);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_ROOMSECRET, secret);
        return this;
    }

    /**
     * Submit the configuration as {@link Form} to the room.
     *
     * @throws NoResponseException if there was no response from the room.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void submitConfigurationForm() throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        if (owners != null) {
            answerForm.setAnswer(MUC_ROOMCONFIG_ROOMOWNERS, JidUtil.toStringList(owners));
        }
        multiUserChat.sendConfigurationForm(answerForm);
    }
}
