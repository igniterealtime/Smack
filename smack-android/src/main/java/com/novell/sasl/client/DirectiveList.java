/* **************************************************************************
 * $OpenLDAP: /com/novell/sasl/client/DirectiveList.java,v 1.4 2005/01/17 15:00:54 sunilk Exp $
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

import java.util.*;
import org.apache.harmony.javax.security.sasl.*;
import java.io.UnsupportedEncodingException;

/**
 * Implements the DirectiveList class whihc will be used by the 
 * DigestMD5SaslClient class
 */
class DirectiveList extends Object
{
    private static final int STATE_LOOKING_FOR_FIRST_DIRECTIVE  = 1;
    private static final int STATE_LOOKING_FOR_DIRECTIVE        = 2;
    private static final int STATE_SCANNING_NAME                = 3;
    private static final int STATE_LOOKING_FOR_EQUALS            = 4;
    private static final int STATE_LOOKING_FOR_VALUE            = 5;
    private static final int STATE_LOOKING_FOR_COMMA            = 6;
    private static final int STATE_SCANNING_QUOTED_STRING_VALUE    = 7;
    private static final int STATE_SCANNING_TOKEN_VALUE            = 8;
    private static final int STATE_NO_UTF8_SUPPORT              = 9;

    private int        m_curPos;
    private int        m_errorPos;
    private String     m_directives;
    private int        m_state;
    private ArrayList  m_directiveList;
    private String     m_curName;
    private int        m_scanStart;

    /**
     *  Constructs a new DirectiveList.
     */
     DirectiveList(
        byte[] directives)
    {
        m_curPos = 0;
        m_state = STATE_LOOKING_FOR_FIRST_DIRECTIVE;
        m_directiveList = new ArrayList(10);
        m_scanStart = 0;
        m_errorPos = -1;
        try
        {
            m_directives = new String(directives, "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            m_state = STATE_NO_UTF8_SUPPORT;
        }
    }

    /**
     * This function takes a US-ASCII character string containing a list of comma
     * separated directives, and parses the string into the individual directives
     * and their values. A directive consists of a token specifying the directive
     * name followed by an equal sign (=) and the directive value. The value is
     * either a token or a quoted string
     *
     * @exception SaslException  If an error Occurs
     */
    void parseDirectives() throws SaslException
    {
        char        prevChar;
        char        currChar;
        int            rc = 0;
        boolean        haveQuotedPair = false;
        String      currentName = "<no name>";

        if (m_state == STATE_NO_UTF8_SUPPORT)
            throw new SaslException("No UTF-8 support on platform");

        prevChar = 0;

        while (m_curPos < m_directives.length())
        {
            currChar = m_directives.charAt(m_curPos);
            switch (m_state)
            {
            case STATE_LOOKING_FOR_FIRST_DIRECTIVE:
            case STATE_LOOKING_FOR_DIRECTIVE:
                if (isWhiteSpace(currChar))
                {
                    break;
                }
                else if (isValidTokenChar(currChar))
                {
                    m_scanStart = m_curPos;
                    m_state = STATE_SCANNING_NAME;
                }
                else
                {
                     m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Invalid name character");
                }
                break;

            case STATE_SCANNING_NAME:
                if (isValidTokenChar(currChar))
                {
                    break;
                }
                else if (isWhiteSpace(currChar))
                {
                    currentName = m_directives.substring(m_scanStart, m_curPos);
                    m_state = STATE_LOOKING_FOR_EQUALS;
                }
                else if ('=' == currChar)
                {
                    currentName = m_directives.substring(m_scanStart, m_curPos);
                    m_state = STATE_LOOKING_FOR_VALUE;
                }
                else
                {
                     m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Invalid name character");
                }
                break;

            case STATE_LOOKING_FOR_EQUALS:
                if (isWhiteSpace(currChar))
                {
                    break;
                }
                else if ('=' == currChar)
                {
                    m_state = STATE_LOOKING_FOR_VALUE;
                }
                else
                {
                    m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Expected equals sign '='.");
                }
                break;

            case STATE_LOOKING_FOR_VALUE:
                if (isWhiteSpace(currChar))
                {
                    break;
                }
                else if ('"' == currChar)
                {
                    m_scanStart = m_curPos+1; /* don't include the quote */
                    m_state = STATE_SCANNING_QUOTED_STRING_VALUE;
                }
                else if (isValidTokenChar(currChar))
                {
                    m_scanStart = m_curPos;
                    m_state = STATE_SCANNING_TOKEN_VALUE;
                }
                else
                {
                    m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Unexpected character");
                }
                break;

            case STATE_SCANNING_TOKEN_VALUE:
                if (isValidTokenChar(currChar))
                {
                    break;
                }
                else if (isWhiteSpace(currChar))
                {
                    addDirective(currentName, false);
                    m_state = STATE_LOOKING_FOR_COMMA;
                }
                else if (',' == currChar)
                {
                    addDirective(currentName, false);
                    m_state = STATE_LOOKING_FOR_DIRECTIVE;
                }
                else
                {
                     m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Invalid value character");
                }
                break;

            case STATE_SCANNING_QUOTED_STRING_VALUE:
                if ('\\' == currChar)
                    haveQuotedPair = true;
                if ( ('"' == currChar) &&
                     ('\\' != prevChar) )
                {
                    addDirective(currentName, haveQuotedPair);
                    haveQuotedPair = false;
                    m_state = STATE_LOOKING_FOR_COMMA;
                }
                break;

            case STATE_LOOKING_FOR_COMMA:
                if (isWhiteSpace(currChar))
                    break;
                else if (currChar == ',')
                    m_state = STATE_LOOKING_FOR_DIRECTIVE;
                else
                {
                    m_errorPos = m_curPos;
                    throw new SaslException("Parse error: Expected a comma.");
                }
                break;
            }
            if (0 != rc)
                break;
            prevChar = currChar;
            m_curPos++;
        } /* end while loop */


        if (rc == 0)
        {
            /* check the ending state */
            switch (m_state)
            {
            case STATE_SCANNING_TOKEN_VALUE:
                addDirective(currentName, false);
                break;

            case STATE_LOOKING_FOR_FIRST_DIRECTIVE:
            case STATE_LOOKING_FOR_COMMA:
                break;

            case STATE_LOOKING_FOR_DIRECTIVE:
                    throw new SaslException("Parse error: Trailing comma.");

            case STATE_SCANNING_NAME:
            case STATE_LOOKING_FOR_EQUALS:
            case STATE_LOOKING_FOR_VALUE:
                    throw new SaslException("Parse error: Missing value.");

            case STATE_SCANNING_QUOTED_STRING_VALUE:
                    throw new SaslException("Parse error: Missing closing quote.");
            }
        }

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
     * @param c  character to be tested
     *
     * @return Returns TRUE if the character is a valid token character.
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
     * @param c  Input charcter to be tested
     *
     * @return Returns TRUE if the character is linear white space (LWS)
     */
    boolean isWhiteSpace(
        char c)
    {
        if ( ('\t' == c) ||  // HORIZONTAL TABULATION.
             ('\n' == c) ||  // LINE FEED.
             ('\r' == c) ||  // CARRIAGE RETURN.
             ('\u0020' == c) )
            return true;

        return false;
    }

    /**
     * This function creates a directive record and adds it to the list, the
     * value will be added later after it is parsed.
     *
     * @param name  Name
     * @param haveQuotedPair true if quoted pair is there else false
     */
    void addDirective(
        String    name,
        boolean   haveQuotedPair)
    {
        String value;
        int    inputIndex;
        int    valueIndex;
        char   valueChar;
        int    type;

        if (!haveQuotedPair)
        {
            value = m_directives.substring(m_scanStart, m_curPos);
        }
        else
        { //copy one character at a time skipping backslash excapes.
            StringBuffer valueBuf = new StringBuffer(m_curPos - m_scanStart);
            valueIndex = 0;
            inputIndex = m_scanStart;
            while (inputIndex < m_curPos)
            {
                if ('\\' == (valueChar = m_directives.charAt(inputIndex)))
                    inputIndex++;
                valueBuf.setCharAt(valueIndex, m_directives.charAt(inputIndex));
                valueIndex++;
                inputIndex++;
            }
            value = new String(valueBuf);
        }

        if (m_state == STATE_SCANNING_QUOTED_STRING_VALUE)
            type = ParsedDirective.QUOTED_STRING_VALUE;
        else
            type = ParsedDirective.TOKEN_VALUE;
        m_directiveList.add(new ParsedDirective(name, value, type));
    }


    /**
     * Returns the List iterator.
     *
     * @return     Returns the Iterator Object for the List.
     */
    Iterator getIterator()
    {
        return m_directiveList.iterator();
    }
}

