/*
 *
 * Copyright 2015-2025 Florian Schmaus
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.muc.MultiUserChatException.MucConfigurationNotSupportedException;
import org.jivesoftware.smackx.xdata.BooleanFormField;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.FilledForm;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.util.JidUtil;

/**
 * Multi-User Chat configuration form manager is used to fill out and submit a {@link FilledForm} used to
 * configure rooms.
 * <p>
 * Room configuration needs either be done right after the room is created and still locked. Or at
 * any later point (see <a href="http://xmpp.org/extensions/xep-0045.html#roomconfig">XEP-45 § 10.2
 * Subsequent Room Configuration</a>). When done with the configuration, call
 * {@link #submitConfigurationForm()}.
 * </p>
 * <p>
 * The manager may not provide all possible configuration options. If you want direct access to the
 * configuration form, use {@link MultiUserChat#getConfigurationForm()} and
 * {@link MultiUserChat#sendConfigurationForm(FillableForm)}.
 * </p>
 */
public class MucConfigFormManager {

    private static final String HASH_ROOMCONFIG = "#roomconfig";

    private static final Logger LOGGER = Logger.getLogger(MucConfigFormManager.class.getName());

    public static final String FORM_TYPE = MultiUserChatConstants.NAMESPACE + HASH_ROOMCONFIG;

                    /**
     * The constant String {@value}.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#owner">XEP-0045 § 10. Owner Use Cases</a>
     */
    public static final String MUC_ROOMCONFIG_ROOMOWNERS = "muc#roomconfig_roomowners";

    /**
     * The constant String {@value}.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#owner">XEP-0045 § 10. Owner Use Cases</a>
     */
    public static final String MUC_ROOMCONFIG_ROOMADMINS = "muc#roomconfig_roomadmins";

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

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_MODERATEDROOM = "muc#roomconfig_moderatedroom";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_PUBLICLYSEARCHABLEROOM = "muc#roomconfig_publicroom";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_ROOMNAME = "muc#roomconfig_roomname";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_ENABLE_PUBLIC_LOGGING = "muc#roomconfig_enablelogging";

    /**
     * The constant String {@value}.
     */
    public static final String MUC_ROOMCONFIG_CHANGE_SUBJECT = "muc#roomconfig_changesubject";

    public static final String MUC_ROOMCONFIG_WHOIS = "muc#roomconfig_whois";

    public static final String MUC_ROOMCONFIG_MAXUSERS = "muc#roomconfig_maxusers";

    private final MultiUserChat multiUserChat;
    private final FillableForm answerForm;
    private final List<Jid> owners;
    private final List<Jid> admins;

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
        this.answerForm = configForm.getFillableForm();

        // Set the local variables according to the fields found in the answer form
        FormField roomOwnersFormField = answerForm.getDataForm().getField(MUC_ROOMCONFIG_ROOMOWNERS);
        if (roomOwnersFormField != null) {
            // Set 'owners' to the currently configured owners
            List<? extends CharSequence> ownerStrings = roomOwnersFormField.getValues();
            owners = new ArrayList<>(ownerStrings.size());
            JidUtil.jidsFrom(ownerStrings, owners, null);
        }
        else {
            // roomowners not supported, this should barely be the case
            owners = null;
        }

        FormField roomAdminsFormField = answerForm.getDataForm().getField(MUC_ROOMCONFIG_ROOMADMINS);
        if (roomAdminsFormField != null) {
            // Set 'admins' to the currently configured admins
            List<? extends CharSequence> adminStrings = roomAdminsFormField.getValues();
            admins = new ArrayList<>(adminStrings.size());
            JidUtil.jidsFrom(adminStrings, admins, null);
        }
        else {
            // roomadmins not supported, this should barely be the case
            admins = null;
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
     * Check if the room supports room admins.
     * @return <code>true</code> if supported, <code>false</code> if not.
     * @see #MUC_ROOMCONFIG_ROOMADMINS
     */
    public boolean supportsRoomAdmins() {
        return admins != null;
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
     * Set the admins of the room.
     *
     * @param newAdmins a collection of JIDs to become the new admins of the room.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the MUC service does not support this option.
     * @see #MUC_ROOMCONFIG_ROOMADMINS
     */
    public MucConfigFormManager setRoomAdmins(Collection<? extends Jid> newAdmins) throws MucConfigurationNotSupportedException {
        if (!supportsRoomAdmins()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ROOMADMINS);
        }
        admins.clear();
        admins.addAll(newAdmins);
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
     * Check if the room supports being moderated in the configuration.
     *
     * @return <code>true</code> if supported, <code>false</code> if not.
     */
    public boolean supportsModeration() {
        return answerForm.hasField(MUC_ROOMCONFIG_MODERATEDROOM);
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
     * Make the room moderated.
     *
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager makeModerated() throws MucConfigurationNotSupportedException {
        return setModerated(true);
    }

    /**
     * Set if the room is members only. Rooms are not members only per default.
     *
     * @param isModerated if the room should be moderated.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setModerated(boolean isModerated) throws MucConfigurationNotSupportedException {
        if (!supportsModeration()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_MODERATEDROOM);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_MODERATEDROOM, isModerated);
        return this;
    }


    /**
     * Check if the room supports its visibility being controlled via configuration.
     *
     * @return <code>true</code> if supported, <code>false</code> if not.
     */
    public boolean supportsPublicRoom() {
        return answerForm.hasField(MUC_ROOMCONFIG_PUBLICLYSEARCHABLEROOM);
    }

    /**
     * Make the room publicly searchable.
     *
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager makePublic() throws MucConfigurationNotSupportedException {
        return setPublic(true);
    }

    /**
     * Make the room hidden (not publicly searchable).
     *
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager makeHidden() throws MucConfigurationNotSupportedException {
        return setPublic(false);
    }

    /**
     * Set if the room is publicly searchable (i.e. visible via discovery requests to the MUC service).
     *
     * @param isPublic if the room should be publicly searchable.
     * @return a reference to this object.
     * @throws MucConfigurationNotSupportedException if the requested MUC configuration is not supported by the MUC service.
     */
    public MucConfigFormManager setPublic(boolean isPublic) throws MucConfigurationNotSupportedException {
        if (!supportsPublicRoom()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_PUBLICLYSEARCHABLEROOM);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_PUBLICLYSEARCHABLEROOM, isPublic);
        return this;
    }

    public boolean supportsRoomname() {
        return answerForm.hasField(MUC_ROOMCONFIG_ROOMNAME);
    }

    public MucConfigFormManager setRoomName(String roomName) throws MucConfigurationNotSupportedException {
        if (!supportsRoomname()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ROOMNAME);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_ROOMNAME, roomName);
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
        if (!supportsPasswordProtected()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_PASSWORDPROTECTEDROOM, isPasswordProtected);
        return this;
    }

    public boolean supportsPublicLogging() {
        return answerForm.hasField(MUC_ROOMCONFIG_ENABLE_PUBLIC_LOGGING);
    }

    public MucConfigFormManager setPublicLogging(boolean enabled) throws MucConfigurationNotSupportedException {
        if (!supportsPublicLogging()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_ENABLE_PUBLIC_LOGGING);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_ENABLE_PUBLIC_LOGGING, enabled);
        return this;
    }

    public MucConfigFormManager enablePublicLogging() throws MucConfigurationNotSupportedException {
        return setPublicLogging(true);
    }

    public MucConfigFormManager disablPublicLogging() throws MucConfigurationNotSupportedException {
        return setPublicLogging(false);
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

    public boolean supportsChangeSubjectByOccupant() {
        return answerForm.hasField(MUC_ROOMCONFIG_CHANGE_SUBJECT);
    }

    public boolean occupantsAreAllowedToChangeSubject() throws MucConfigurationNotSupportedException {
        if (!supportsChangeSubjectByOccupant()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_CHANGE_SUBJECT);
        }
        return answerForm.getField(MUC_ROOMCONFIG_CHANGE_SUBJECT).ifPossibleAsOrThrow(BooleanFormField.class).getValueAsBoolean();
    }

    public MucConfigFormManager setChangeSubjectByOccupant(boolean enabled) throws MucConfigurationNotSupportedException {
        if (!supportsChangeSubjectByOccupant()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_CHANGE_SUBJECT);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_CHANGE_SUBJECT, enabled);
        return this;
    }

    public MucConfigFormManager allowOccupantsToChangeSubject() throws MucConfigurationNotSupportedException {
        return setChangeSubjectByOccupant(true);
    }

    public MucConfigFormManager disallowOccupantsToChangeSubject() throws MucConfigurationNotSupportedException {
        return setChangeSubjectByOccupant(false);
    }

    enum WhoisAllowedBy {
        moderators,
        anyone,
    }

    public boolean supportsWhoisAllowedBy() {
        return answerForm.hasField(MUC_ROOMCONFIG_WHOIS);
    }

    public MucConfigFormManager setWhoisAllowedBy(WhoisAllowedBy whoisAllowedBy)
                    throws MucConfigurationNotSupportedException {
        if (!supportsWhoisAllowedBy()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_WHOIS);
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_WHOIS, whoisAllowedBy.name());
        return this;
    }

    public boolean supportsMaxUsers() {
        return answerForm.hasField(MUC_ROOMCONFIG_MAXUSERS);
    }

    public List<Integer> getPossibleMaxUsersValues() throws MucConfigurationNotSupportedException {
        if (!supportsMaxUsers()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_MAXUSERS);
        }
        return answerForm.getField(MUC_ROOMCONFIG_MAXUSERS)
                        .getValuesAsString()
                        .stream()
                        .map(s -> Integer.valueOf(s))
                        .collect(Collectors.toList());
    }

    public MucConfigFormManager setMaxUsers(int maxUsers) throws MucConfigurationNotSupportedException {
        if (!supportsMaxUsers()) {
            throw new MucConfigurationNotSupportedException(MUC_ROOMCONFIG_MAXUSERS);
        }
        if (maxUsers < 1) {
            throw new IllegalArgumentException();
        }
        answerForm.setAnswer(MUC_ROOMCONFIG_MAXUSERS, maxUsers);
        return this;
    }

    /**
     * Submit the configuration as {@link FilledForm} to the room.
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
        if (admins != null) {
            answerForm.setAnswer(MUC_ROOMCONFIG_ROOMADMINS, JidUtil.toStringList(admins));
        }
        multiUserChat.sendConfigurationForm(answerForm);
    }

    public void cancel() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        var cancelDataForm = DataForm.builder(DataForm.Type.cancel).build();
        multiUserChat.sendAsMucOwner(cancelDataForm);
    }

    public interface MucConfigApplier {
        void apply(MucConfigFormManager manager)
                        throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, MucConfigurationNotSupportedException;
    }

    public MultiUserChat applyAndSubmit(MucConfigApplier applier)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, MucConfigurationNotSupportedException {
        try {
            applier.apply(this);
            submitConfigurationForm();
        } catch (XMPPErrorException | InterruptedException | MucConfigurationNotSupportedException e) {
            try {
                cancel();
            } catch (NoResponseException | XMPPErrorException | NotConnectedException
                            | InterruptedException cancelException) {
                LOGGER.log(Level.SEVERE, "Exception while canceling MUC configuration for " + multiUserChat, e);
            }
            throw e;
        }

        return multiUserChat;
    }
}
