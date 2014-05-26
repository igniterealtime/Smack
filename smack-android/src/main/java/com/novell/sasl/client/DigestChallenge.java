/* **************************************************************************
 * $OpenLDAP: /com/novell/sasl/client/DigestChallenge.java,v 1.3 2005/01/17 15:00:54 sunilk Exp $
 *
 * Copyright (C) 2003 Novell, Inc. All Rights Reserved.
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

import java.util.*;
import org.apache.harmony.javax.security.sasl.*;

/**
 * Implements the DigestChallenge class which will be used by the
 * DigestMD5SaslClient class
 */
class DigestChallenge extends Object
{
    public static final int QOP_AUTH           =    0x01;
    public static final int QOP_AUTH_INT       =    0x02;
    public static final int QOP_AUTH_CONF       =    0x04;
    public static final int QOP_UNRECOGNIZED   =    0x08;

    private static final int CIPHER_3DES          = 0x01;
    private static final int CIPHER_DES           = 0x02;
    private static final int CIPHER_RC4_40        = 0x04;
    private static final int CIPHER_RC4           = 0x08;
    private static final int CIPHER_RC4_56        = 0x10;
    private static final int CIPHER_UNRECOGNIZED  = 0x20;
    private static final int CIPHER_RECOGNIZED_MASK =
     CIPHER_3DES | CIPHER_DES | CIPHER_RC4_40 | CIPHER_RC4 | CIPHER_RC4_56;

    private ArrayList m_realms;
    private String    m_nonce;
    private int       m_qop;
    private boolean   m_staleFlag;
    private int       m_maxBuf;
    private String    m_characterSet;
    private String    m_algorithm;
    private int       m_cipherOptions;

    DigestChallenge(
        byte[] challenge)
            throws SaslException
    {
        m_realms = new ArrayList(5);
        m_nonce = null;
        m_qop = 0;
        m_staleFlag = false;
        m_maxBuf = -1;
        m_characterSet = null;
        m_algorithm = null;
        m_cipherOptions = 0;

        DirectiveList dirList = new DirectiveList(challenge);
        try
        {
            dirList.parseDirectives();
            checkSemantics(dirList);
        }
        catch (SaslException e)
        {
        }
    }

    /**
     * Checks the semantics of the directives in the directive list as parsed
     * from the digest challenge byte array.
     *
     * @param dirList  the list of directives parsed from the digest challenge
     *
     * @exception SaslException   If a semantic error occurs
     */
    void checkSemantics(
        DirectiveList dirList) throws SaslException
    {
    Iterator        directives = dirList.getIterator();
    ParsedDirective directive;
    String          name;

    while (directives.hasNext())
    {
        directive = (ParsedDirective)directives.next();
        name = directive.getName();
        if (name.equals("realm"))
            handleRealm(directive);
        else if (name.equals("nonce"))
            handleNonce(directive);
        else if (name.equals("qop"))
            handleQop(directive);
        else if (name.equals("maxbuf"))
            handleMaxbuf(directive);
        else if (name.equals("charset"))
            handleCharset(directive);
        else if (name.equals("algorithm"))
            handleAlgorithm(directive);
        else if (name.equals("cipher"))
            handleCipher(directive);
        else if (name.equals("stale"))
            handleStale(directive);
    }

    /* post semantic check */
    if (-1 == m_maxBuf)
        m_maxBuf = 65536;

    if (m_qop == 0)
        m_qop = QOP_AUTH;
    else if ( (m_qop & QOP_AUTH) != QOP_AUTH )
        throw new SaslException("Only qop-auth is supported by client");
    else if ( ((m_qop & QOP_AUTH_CONF) == QOP_AUTH_CONF) &&
              (0 == (m_cipherOptions & CIPHER_RECOGNIZED_MASK)) )
        throw new SaslException("Invalid cipher options");
    else if (null == m_nonce)
        throw new SaslException("Missing nonce directive");
    else if (m_staleFlag)
        throw new SaslException("Unexpected stale flag");
    else if ( null == m_algorithm )
        throw new SaslException("Missing algorithm directive");
    }

    /**
     * This function implements the semenatics of the nonce directive.
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException   If an error occurs due to too many nonce
     *                             values
     */
    void handleNonce(
        ParsedDirective  pd) throws SaslException
    {
        if (null != m_nonce)
            throw new SaslException("Too many nonce values.");

        m_nonce = pd.getValue();
    }

    /**
     * This function implements the semenatics of the realm directive.
     *
     * @param      pd   ParsedDirective
     */
    void handleRealm(
        ParsedDirective  pd)
    {
        m_realms.add(pd.getValue());
    }

    /**
     * This function implements the semenatics of the qop (quality of protection)
     * directive. The value of the qop directive is as defined below:
     *      qop-options =     "qop" "=" <"> qop-list <">
     *      qop-list    =     1#qop-value
     *      qop-value    =     "auth" | "auth-int"  | "auth-conf" | token
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException   If an error occurs due to too many qop
     *                             directives
     */
    void handleQop(
        ParsedDirective  pd) throws SaslException
    {
        String       token;
        TokenParser  parser;

        if (m_qop != 0)
            throw new SaslException("Too many qop directives.");

        parser = new TokenParser(pd.getValue());
        for (token = parser.parseToken();
             token != null;
             token = parser.parseToken())
        {
            if (token.equals("auth"))
                  m_qop |= QOP_AUTH;
              else if (token.equals("auth-int"))
                  m_qop |= QOP_AUTH_INT;
            else if (token.equals("auth-conf"))
                m_qop |= QOP_AUTH_CONF;
            else
                m_qop |= QOP_UNRECOGNIZED;
        }
    }

    /**
     * This function implements the semenatics of the Maxbuf directive.
     * the value is defined as: 1*DIGIT
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException If an error occur    
     */
    void handleMaxbuf(
        ParsedDirective  pd) throws SaslException
    {
        if (-1 != m_maxBuf) /*it's initialized to -1 */
            throw new SaslException("Too many maxBuf directives.");

        m_maxBuf = Integer.parseInt(pd.getValue());

        if (0 == m_maxBuf)
            throw new SaslException("Max buf value must be greater than zero.");
    }

    /**
     * This function implements the semenatics of the charset directive.
     * the value is defined as: 1*DIGIT
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException If an error occurs dur to too many charset
     *                           directives or Invalid character encoding
     *                           directive
     */
    void handleCharset(
        ParsedDirective  pd) throws SaslException
    {
        if (null != m_characterSet)
            throw new SaslException("Too many charset directives.");

        m_characterSet = pd.getValue();

        if (!m_characterSet.equals("utf-8"))
            throw new SaslException("Invalid character encoding directive");
    }

    /**
     * This function implements the semenatics of the charset directive.
     * the value is defined as: 1*DIGIT
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException If an error occurs due to too many algorith
     *                           directive or Invalid algorithm directive
     *                           value
     */
    void handleAlgorithm(
        ParsedDirective  pd) throws SaslException
    {
        if (null != m_algorithm)
            throw new SaslException("Too many algorithm directives.");

          m_algorithm = pd.getValue();

        if (!"md5-sess".equals(m_algorithm))
            throw new SaslException("Invalid algorithm directive value: " +
                                    m_algorithm);
    }

    /**
     * This function implements the semenatics of the cipher-opts directive
     * directive. The value of the qop directive is as defined below:
     *      qop-options =     "qop" "=" <"> qop-list <">
     *      qop-list    =     1#qop-value
     *      qop-value    =     "auth" | "auth-int"  | "auth-conf" | token
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException If an error occurs due to Too many cipher
     *                           directives 
     */
    void handleCipher(
        ParsedDirective  pd) throws SaslException
    {
        String  token;
        TokenParser parser;

        if (0 != m_cipherOptions)
            throw new SaslException("Too many cipher directives.");

        parser = new TokenParser(pd.getValue());
        token = parser.parseToken();
        for (token = parser.parseToken();
             token != null;
             token = parser.parseToken())
        {
              if ("3des".equals(token))
                  m_cipherOptions |= CIPHER_3DES;
              else if ("des".equals(token))
                  m_cipherOptions |= CIPHER_DES;
            else if ("rc4-40".equals(token))
                m_cipherOptions |= CIPHER_RC4_40;
            else if ("rc4".equals(token))
                m_cipherOptions |= CIPHER_RC4;
            else if ("rc4-56".equals(token))
                m_cipherOptions |= CIPHER_RC4_56;
            else
                m_cipherOptions |= CIPHER_UNRECOGNIZED;
        }

        if (m_cipherOptions == 0)
            m_cipherOptions = CIPHER_UNRECOGNIZED;
    }

    /**
     * This function implements the semenatics of the stale directive.
     *
     * @param      pd   ParsedDirective
     *
     * @exception  SaslException If an error occurs due to Too many stale
     *                           directives or Invalid stale directive value
     */
    void handleStale(
        ParsedDirective  pd) throws SaslException
    {
        if (false != m_staleFlag)
            throw new SaslException("Too many stale directives.");

        if ("true".equals(pd.getValue()))
            m_staleFlag = true;
        else
            throw new SaslException("Invalid stale directive value: " +
                                    pd.getValue());
    }

    /**
     * Return the list of the All the Realms
     *
     * @return  List of all the realms 
     */
    public ArrayList getRealms()
    {
        return m_realms;
    }

    /**
     * @return Returns the Nonce
     */
    public String getNonce()
    {
        return m_nonce;
    }

    /**
     * Return the quality-of-protection
     * 
     * @return The quality-of-protection
     */
    public int getQop()
    {
        return m_qop;
    }

    /**
     * @return The state of the Staleflag
     */
    public boolean getStaleFlag()
    {
        return m_staleFlag;
    }

    /**
     * @return The Maximum Buffer value
     */
    public int getMaxBuf()
    {
        return m_maxBuf;
    }

    /**
     * @return character set values as string
     */
    public String getCharacterSet()
    {
        return m_characterSet;
    }

    /**
     * @return The String value of the algorithm
     */
    public String getAlgorithm()
    {
        return m_algorithm;
    }

    /**
     * @return The cipher options
     */
    public int getCipherOptions()
    {
        return m_cipherOptions;
    }
}

