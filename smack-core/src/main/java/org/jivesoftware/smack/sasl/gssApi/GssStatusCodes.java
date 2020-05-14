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
package org.jivesoftware.smack.sasl.gssApi;

public class GssStatusCodes {

    public enum FatalErrorCodes {
        GSS_S_BAD_BINDINGS,             // channel binding mismatch
        GSS_S_BAD_MECH,                 // unsupported mechanism requested
        GSS_S_BAD_NAME,                 // invalid name provided
        GSS_S_BAD_NAMETYPE,             // name of unsupported type provided
        GSS_S_BAD_STATUS,               // invalid input status selector
        GSS_S_BAD_SIG,                  // token had invalid integrity check
        GSS_S_BAD_MIC,                  // preferred alias for GSS_S_BAD_SIG
        GSS_S_CONTEXT_EXPIRED,          // specified security context expired
        GSS_S_CREDENTIALS_EXPIRED,      // expired credentials detected
        GSS_S_DEFECTIVE_CREDENTIAL,     // defective credential detected
        GSS_S_DEFECTIVE_TOKEN,          // defective token detected
        GSS_S_FAILURE,                  // failure, unspecified at GSS-API level
        GSS_S_NO_CONTEXT,               // no valid security context specified
        GSS_S_NO_CRED,                  // no valid credentials provided
        GSS_S_BAD_QOP,                  // unsupported QOP value
        GSS_S_UNAUTHORIZED,             // operation unauthorized
        GSS_S_UNAVAILABLE,              // operation unavailable
        GSS_S_DUPLICATE_ELEMENT,        // duplicate credential element requested
        GSS_S_NAME_NOT_MN               // name contains multi-mechanism elements
    }

    public enum InformatoryStatusCodes {
        GSS_S_COMPLETE,                 // normal completion
        GSS_S_CONTINUE_NEEDED,          // continuation call to routine required
        GSS_S_DUPLICATE_TOKEN,          // duplicate per-message token detected
        GSS_S_OLD_TOKEN,                // timed-out per-message token detected
        GSS_S_UNSEQ_TOKEN,              // reordered (early) per-message token detected
        GSS_S_GAP_TOKEN                 // skipped predecessor token(s) detected
    }

    public static String OID_SimplePublicKeyGssApi = "1.3.6.1.5.5.1.1";
    public static String OID_Kerberos_V5_GssAPI = " 1.2.840.113554.1.2.2";

}
