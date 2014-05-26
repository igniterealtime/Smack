/* **************************************************************************
 * $OpenLDAP: /com/novell/sasl/client/ResponseAuth.java,v 1.3 2005/01/17 15:00:54 sunilk Exp $
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

/**
 * Implements the ResponseAuth class used by the DigestMD5SaslClient mechanism
 */
class ResponseAuth
{

    private String m_responseValue;

    ResponseAuth(
        byte[] responseAuth)
            throws SaslException
    {
        m_responseValue = null;

        DirectiveList dirList = new DirectiveList(responseAuth);
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
            if (name.equals("rspauth"))
                m_responseValue = directive.getValue();
        }

        /* post semantic check */
        if (m_responseValue == null)
            throw new SaslException("Missing response-auth directive.");
    }

    /**
     * returns the ResponseValue
     *
     * @return the ResponseValue as a String.
     */
    public String getResponseValue()
    {
        return m_responseValue;
    }
}

