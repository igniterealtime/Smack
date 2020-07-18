/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static org.jivesoftware.smack.util.StringUtils.UNAMBIGUOUS_NUMBERS_AND_LETTERS_STRING;

import java.util.regex.Pattern;

/**
 * Represents a secret key backup passphrase whose format is described in XEP-0373 §5.3.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0373.html#backup-encryption">
 *      XEP-0373 §5.4 Encrypting the Secret Key Backup</a>
 */
public class OpenPgpSecretKeyBackupPassphrase implements CharSequence {

    private static final Pattern PASSPHRASE_PATTERN = Pattern.compile(
            "^([" + UNAMBIGUOUS_NUMBERS_AND_LETTERS_STRING + "]{4}-){5}" +
                    "[" + UNAMBIGUOUS_NUMBERS_AND_LETTERS_STRING + "]{4}$");

    private final String passphrase;

    public OpenPgpSecretKeyBackupPassphrase(String passphrase) {
        if (!PASSPHRASE_PATTERN.matcher(passphrase).matches()) {
            throw new IllegalArgumentException("Passphrase must be 24 upper case letters and numbers from the english " +
                    "alphabet without 'O' and '0', divided into blocks of 4 and separated with dashes ('-').");
        }
        this.passphrase = passphrase;
    }

    @Override
    public int length() {
        return passphrase.length();
    }

    @Override
    public char charAt(int i) {
        return passphrase.charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return passphrase.subSequence(i, i1);
    }

    @Override
    public String toString() {
        return passphrase;
    }
}
