/* **************************************************************************
 * $OpenLDAP: /com/novell/sasl/client/ExternalSaslClient.java $
 *
 * Copyright (C) 2002 Novell, Inc. All Rights Reserved.
 *
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE, MODIFICATION, AND REDISTRIBUTION OF THIS WORK IS SUBJECT
 * TO VERSION 2.0.1 OF THE OPENLDAP PUBLIC LICENSE, A COPY OF WHICH IS
 * AVAILABLE AT HTTP://WWW.OPENLDAP.ORG/LICENSE.HTML OR IN THE FILE "LICENSE"
 * IN THE TOP-LEVEL DIRECTORY OF THE DISTRIBUTION. ANY USE OR EXPLOITATION
 * OF THIS WORK OTHER THAN AS AUTHORIZED IN VERSION 2.0.1 OF THE OPENLDAP
 * PUBLIC LICENSE, OR OTHER PRIOR WRITTEN CONSENT FROM NOVELL, COULD SUBJECT
 * THE PERPETRATOR TO CRIMINAL AND CIVIL LIABILITY.
 ******************************************************************************/
package com.novell.sasl.client;

import org.apache.harmony.javax.security.sasl.*;
import org.apache.harmony.javax.security.auth.callback.*;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.*;
/**
 * Implements the ExternalSaslClient mechanism.
 */
public class ExternalSaslClient extends Object implements SaslClient
{
    private String           m_authorizationId = "";
    private String           m_protocol = "";
    private String           m_serverName = "";
    private Map              m_props;
    private CallbackHandler  m_cbh;
    private int              m_state;

    private static final int   STATE_INITIAL = 0;
    private static final int   STATE_VALID_SERVER_RESPONSE = 1;
    private static final int   STATE_INVALID_SERVER_RESPONSE = 2;
    private static final int   STATE_DISPOSED = 3;

    /**
     * Creates an ExternalSaslClient object using the parameters supplied.
     * Assumes that the QOP, STRENGTH, and SERVER_AUTH properties are
     * contained in props
     *
     * @param authorizationId  The possibly null protocol-dependent
     *                     identification to be used for authorization. If
     *                     null or empty, the server derives an authorization
     *                     ID from the client's authentication credentials.
     *                     When the SASL authentication completes
     *                     successfully, the specified entity is granted
     *                     access.
     *
     * @param protocol     The non-null string name of the protocol for which
     *                     the authentication is being performed (e.g. "ldap")
     *
     * @param serverName   The non-null fully qualified host name of the server
     *                     to authenticate to
     *
     * @param props        The possibly null set of properties used to select
     *                     the SASL mechanism and to configure the
     *                     authentication exchange of the selected mechanism.
     *                     See the Sasl class for a list of standard properties.
     *                     Other, possibly mechanism-specific, properties can
     *                     be included. Properties not relevant to the selected
     *                     mechanism are ignored.
     *
     * @param cbh          The possibly null callback handler to used by the
     *                     SASL mechanisms to get further information from the
     *                     application/library to complete the authentication.
     *                     For example, a SASL mechanism might require the
     *                     authentication ID, password and realm from the
     *                     caller. The authentication ID is requested by using
     *                     a NameCallback. The password is requested by using
     *                     a PasswordCallback. The realm is requested by using
     *                     a RealmChoiceCallback if there is a list of realms
     *                     to choose from, and by using a RealmCallback if the
     *                     realm must be entered.
     *
     * @return            A possibly null SaslClient created using the
     *                     parameters supplied. If null, this factory cannot
     *                     produce a SaslClient using the parameters supplied.
     *
     * @exception SaslException  If a SaslClient instance cannot be created
     *                     because of an error
     */
    public static SaslClient getClient(
        String          authorizationId,
        String          protocol,
        String          serverName,
        Map             props,
        CallbackHandler cbh)
    {
        return new ExternalSaslClient(authorizationId, protocol,
                                       serverName, props, cbh);
    }

    /**
     * Creates an ExternalSaslClient object using the parameters supplied.
     * Assumes that the QOP, STRENGTH, and SERVER_AUTH properties are
     * contained in props
     *
     * @param authorizationId  The possibly null protocol-dependent
     *                     identification to be used for authorization. If
     *                     null or empty, the server derives an authorization
     *                     ID from the client's authentication credentials.
     *                     When the SASL authentication completes
     *                     successfully, the specified entity is granted
     *                     access.
     *
     * @param protocol     The non-null string name of the protocol for which
     *                     the authentication is being performed (e.g. "ldap")
     *
     * @param serverName   The non-null fully qualified host name of the server
     *                     to authenticate to
     *
     * @param props        The possibly null set of properties used to select
     *                     the SASL mechanism and to configure the
     *                     authentication exchange of the selected mechanism.
     *                     See the Sasl class for a list of standard properties.
     *                     Other, possibly mechanism-specific, properties can
     *                     be included. Properties not relevant to the selected
     *                     mechanism are ignored.
     *
     * @param cbh          The possibly null callback handler to used by the
     *                     SASL mechanisms to get further information from the
     *                     application/library to complete the authentication.
     *                     For example, a SASL mechanism might require the
     *                     authentication ID, password and realm from the
     *                     caller. The authentication ID is requested by using
     *                     a NameCallback. The password is requested by using
     *                     a PasswordCallback. The realm is requested by using
     *                     a RealmChoiceCallback if there is a list of realms
     *                     to choose from, and by using a RealmCallback if the
     *                     realm must be entered.
     *
     */
    private  ExternalSaslClient(
        String          authorizationId,
        String          protocol,
        String          serverName,
        Map             props,
        CallbackHandler cbh)
    {
        m_authorizationId = authorizationId;
        m_protocol = protocol;
        m_serverName = serverName;
        m_props = props;
        m_cbh = cbh;

        m_state = STATE_INITIAL;
    }

    /**
     * Determines if this mechanism has an optional initial response. If true,
     * caller should call evaluateChallenge() with an empty array to get the
     * initial response.
     *
     * @return  true if this mechanism has an initial response
     */
    public boolean hasInitialResponse()
    {
        return false;
    }

    /**
     * Determines if the authentication exchange has completed. This method
     * may be called at any time, but typically, it will not be called until
     * the caller has received indication from the server (in a protocol-
     * specific manner) that the exchange has completed.
     *
     * @return  true if the authentication exchange has completed;
     *           false otherwise.
     */
    public boolean isComplete()
    {
        if ((m_state == STATE_VALID_SERVER_RESPONSE) ||
            (m_state == STATE_INVALID_SERVER_RESPONSE) ||
            (m_state == STATE_DISPOSED))
            return true;
        else
            return false;
    }

   /**
    * Unwraps a byte array received from the server. This method can be called
    * only after the authentication exchange has completed (i.e., when
    * isComplete() returns true) and only if the authentication exchange has
    * negotiated integrity and/or privacy as the quality of protection;
    * otherwise, an IllegalStateException is thrown.
    *
    * incoming is the contents of the SASL buffer as defined in RFC 2222
    * without the leading four octet field that represents the length.
    * offset and len specify the portion of incoming to use.
    *
    * @param incoming   A non-null byte array containing the encoded bytes
    *                   from the server
    * @param offset     The starting position at incoming of the bytes to use
    *
    * @param len        The number of bytes from incoming to use
    *
    * @return           A non-null byte array containing the decoded bytes
    *
    */
    public byte[] unwrap(
        byte[] incoming,
        int    offset,
        int    len)
            throws SaslException
    {
        throw new IllegalStateException(
         "unwrap: QOP has neither integrity nor privacy>");
    }

   /**
    * Wraps a byte array to be sent to the server. This method can be called
    * only after the authentication exchange has completed (i.e., when
    * isComplete() returns true) and only if the authentication exchange has
    * negotiated integrity and/or privacy as the quality of protection;
    * otherwise, an IllegalStateException is thrown.
    *
    * The result of this method will make up the contents of the SASL buffer as
    * defined in RFC 2222 without the leading four octet field that represents
    * the length. offset and len specify the portion of outgoing to use.
    *
    * @param outgoing   A non-null byte array containing the bytes to encode
    * @param offset     The starting position at outgoing of the bytes to use
    * @param len        The number of bytes from outgoing to use
    *
    * @return A non-null byte array containing the encoded bytes
    *
    * @exception SaslException  if incoming cannot be successfully unwrapped.
    *
    * @exception IllegalStateException   if the authentication exchange has
    *                   not completed, or if the negotiated quality of
    *                   protection has neither integrity nor privacy.
    */
    public byte[] wrap(
        byte[]  outgoing,
        int     offset,
        int     len)
            throws SaslException
    {
        throw new IllegalStateException(
         "wrap: QOP has neither integrity nor privacy>");
    }

    /**
    * Retrieves the negotiated property. This method can be called only after
    * the authentication exchange has completed (i.e., when isComplete()
    * returns true); otherwise, an IllegalStateException is thrown.
    *
    * @param propName   The non-null property name
    *
    * @return  The value of the negotiated property. If null, the property was
    *          not negotiated or is not applicable to this mechanism.
    *
    * @exception IllegalStateException   if this authentication exchange has
    *                                    not completed
    */
    public Object getNegotiatedProperty(
        String propName)
    {
        if (m_state != STATE_VALID_SERVER_RESPONSE)
            throw new IllegalStateException(
             "getNegotiatedProperty: authentication exchange not complete.");

        if (Sasl.QOP.equals(propName))
            return "auth";
        else
            return null;
    }

    /**
     * Disposes of any system resources or security-sensitive information the
     * SaslClient might be using. Invoking this method invalidates the
     * SaslClient instance. This method is idempotent.
     *
     * @exception SaslException - if a problem was encountered while disposing of the resources
     */
    public void dispose()
            throws SaslException
    {
        if (m_state != STATE_DISPOSED)
        {
            m_state = STATE_DISPOSED;
        }
    }

    /**
     * Evaluates the challenge data and generates a response. If a challenge
     * is received from the server during the authentication process, this
     * method is called to prepare an appropriate next response to submit to
     * the server.
     *
     * @param challenge  The non-null challenge sent from the server. The
     *                   challenge array may have zero length.
     *
     * @return    The possibly null reponse to send to the server. It is null
     *            if the challenge accompanied a "SUCCESS" status and the
     *            challenge only contains data for the client to update its
     *            state and no response needs to be sent to the server.
     *            The response is a zero-length byte array if the client is to
     *            send a response with no data.
     *
     * @exception SaslException   If an error occurred while processing the
     *                            challenge or generating a response.
     */
    public byte[] evaluateChallenge(
        byte[] challenge)
            throws SaslException
    {
        byte[] response = null;

        switch (m_state)
        {
        case STATE_INITIAL:
            if (challenge.length != 0)
            {
                m_state = STATE_INVALID_SERVER_RESPONSE;
                throw new SaslException("Unexpected non-zero length response.");
            }
            else
                m_state = STATE_VALID_SERVER_RESPONSE;
            break;
        case STATE_INVALID_SERVER_RESPONSE:
            throw new SaslException("Authentication sequence is complete");
        case STATE_DISPOSED:
            throw new SaslException("Client has been disposed");
        default:
            throw new SaslException("Unknown client state.");
        }

        return response;
    }

    /**
    * Returns the IANA-registered mechanism name of this SASL client.
    *  (e.g. "CRAM-MD5", "GSSAPI")

    * @return  "DIGEST-MD5"the IANA-registered mechanism name of this SASL client.
    */
    public String getMechanismName()
    {
        return "EXTERNAL";
    }

} //end class ExternalSaslClient

