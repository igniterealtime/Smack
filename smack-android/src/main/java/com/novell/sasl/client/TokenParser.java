/* **************************************************************************
 * $OpenLDAP: /com/novell/sasl/client/TokenParser.java,v 1.3 2005/01/17 15:00:54 sunilk Exp $
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
/**
 * The TokenParser class will parse individual tokens from a list of tokens that
 * are a directive value for a DigestMD5 authentication.The tokens are separated
 * commas.
 */
class TokenParser extends Object
{
    private static final int STATE_LOOKING_FOR_FIRST_TOKEN = 1;
    private static final int STATE_LOOKING_FOR_TOKEN       = 2;
    private static final int STATE_SCANNING_TOKEN          = 3;
    private static final int STATE_LOOKING_FOR_COMMA       = 4;
    private static final int STATE_PARSING_ERROR           = 5;
    private static final int STATE_DONE                    = 6;

    private int        m_curPos;
    private int     m_scanStart;
    private int     m_state;
    private String  m_tokens;


    TokenParser(
        String tokens)
    {
        m_tokens = tokens;
        m_curPos = 0;
        m_scanStart = 0;
        m_state =  STATE_LOOKING_FOR_FIRST_TOKEN;
    }

    /**
     * This function parses the next token from the tokens string and returns
     * it as a string. If there are no more tokens a null reference is returned.
     *
     * @return  the parsed token or a null reference if there are no more
     * tokens
     *
     * @exception  SASLException if an error occurs while parsing
     */
    String parseToken() throws SaslException
    {
        char    currChar;
        String  token = null;


        if (m_state == STATE_DONE)
            return null;

        while (m_curPos < m_tokens.length() && (token == null))
        {
            currChar = m_tokens.charAt(m_curPos);
            switch (m_state)
            {
            case STATE_LOOKING_FOR_FIRST_TOKEN:
            case STATE_LOOKING_FOR_TOKEN:
                if (isWhiteSpace(currChar))
                {
                    break;
                }
                else if (isValidTokenChar(currChar))
                {
                    m_scanStart = m_curPos;
                    m_state = STATE_SCANNING_TOKEN;
                }
                else
                {
                    m_state = STATE_PARSING_ERROR;
                    throw new SaslException("Invalid token character at position " + m_curPos);
                }
                break;

            case STATE_SCANNING_TOKEN:
                if (isValidTokenChar(currChar))
                {
                    break;
                }
                else if (isWhiteSpace(currChar))
                {
                    token = m_tokens.substring(m_scanStart, m_curPos);
                    m_state = STATE_LOOKING_FOR_COMMA;
                }
                else if (',' == currChar)
                {
                    token = m_tokens.substring(m_scanStart, m_curPos);
                    m_state = STATE_LOOKING_FOR_TOKEN;
                }
                else
                {
                    m_state = STATE_PARSING_ERROR;
                    throw new SaslException("Invalid token character at position " + m_curPos);
                }
                break;


            case STATE_LOOKING_FOR_COMMA:
                if (isWhiteSpace(currChar))
                    break;
                else if (currChar == ',')
                    m_state = STATE_LOOKING_FOR_TOKEN;
                else
                {
                    m_state = STATE_PARSING_ERROR;
                    throw new SaslException("Expected a comma, found '" +
                                            currChar + "' at postion " +
                                            m_curPos);
                }
                break;
            }
            m_curPos++;
        } /* end while loop */

        if (token == null)
        {    /* check the ending state */
            switch (m_state)
            {
            case STATE_SCANNING_TOKEN:
                token = m_tokens.substring(m_scanStart);
                m_state = STATE_DONE;
                break;

            case STATE_LOOKING_FOR_FIRST_TOKEN:
            case STATE_LOOKING_FOR_COMMA:
                break;

            case STATE_LOOKING_FOR_TOKEN:
                throw new SaslException("Trialing comma");
            }
        }

        return token;
    }

    /**
     * This function returns TRUE if the character is a valid token character.
     *
     *     token          = 1*<any CHAR except CTLs or separators>
     *
     *      separators     = "(" | ")" | "<" | ">" | "@"
     *                     | "," | ";" | ":" | "\" | <">
     *                     | "/" | "[" | "]" | "?" | "="
     *                     | "{" | "}" | SP | HT
     *
     *      CTL            = <any US-ASCII control character
     *                       (octets 0 - 31) and DEL (127)>
     *
     *      CHAR           = <any US-ASCII character (octets 0 - 127)>
     *
     * @param c  character to be validated
     *
     * @return True if character is valid Token character else it returns 
     * false
     */
    boolean isValidTokenChar(
        char c)
    {
        if ( ( (c >= '\u0000') && (c <='\u0020') ) ||
             ( (c >= '\u003a') && (c <= '\u0040') ) ||
             ( (c >= '\u005b') && (c <= '\u005d') ) ||
             ('\u002c' == c) ||
             ('\u0025' == c) ||
             ('\u0028' == c) ||
             ('\u0029' == c) ||
             ('\u007b' == c) ||
             ('\u007d' == c) ||
             ('\u007f' == c) )
            return false;

        return true;
    }

    /**
     * This function returns TRUE if the character is linear white space (LWS).
     *         LWS = [CRLF] 1*( SP | HT )
     *
     * @param c  character to be validated
     *
     * @return True if character is liner whitespace else it returns false
     */
    boolean isWhiteSpace(
        char c)
    {
        if ( ('\t' == c) || // HORIZONTAL TABULATION.
             ('\n' == c) || // LINE FEED.
             ('\r' == c) || // CARRIAGE RETURN.
             ('\u0020' == c) )
            return true;

        return false;
    }

}

