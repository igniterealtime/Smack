<%--
  - $RCSfile$
  - $Revision$
  - $Date$
  -
  - Copyright (C) 2002-2003 Jive Software. 
  - 
  - The Jive Software License (based on Apache Software License, Version 1.1)
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions
  - are met:
  -
  - 1. Redistributions of source code must retain the above copyright
  -    notice, this list of conditions and the following disclaimer.
  -
  - 2. Redistributions in binary form must reproduce the above copyright
  -    notice, this list of conditions and the following disclaimer in
  -    the documentation and/or other materials provided with the
  -    distribution.
  -
  - 3. The end-user documentation included with the redistribution,
  -    if any, must include the following acknowledgment:
  -       "This product includes software developed by
  -        Jive Software (http://www.jivesoftware.com)."
  -    Alternately, this acknowledgment may appear in the software itself,
  -    if and wherever such third-party acknowledgments normally appear.
  -
  - 4. The names "Smack" and "Jive Software" must not be used to
  -    endorse or promote products derived from this software without
  -    prior written permission. For written permission, please
  -    contact webmaster@jivesoftware.com.
  -
  - 5. Products derived from this software may not be called "Smack",
  -    nor may "Smack" appear in their name, without prior written
  -    permission of Jive Software.
  -
  - THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  - WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  - OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  - DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
  - ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  - SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  - LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
  - USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  - OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  - OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  - SUCH DAMAGE.
 --%>
<%@ page import="java.util.*,
                org.jivesoftware.smack.*,
                org.jivesoftware.smack.packet.*,
                org.jivesoftware.smack.util.*"%>
<%@ include file="global.jsp" %>
<%
    // If we don't have a valid connection then proceed to login
    TCPConnection conn = (XMPPConnection) session.getAttribute("connection");
    if (conn == null || !conn.isConnected()) {
        response.sendRedirect("login.jsp");
        return;
    }
    Roster roster = conn.getRoster();

    // Get parameters
    String user = getParameter(request, "user");
    String nickname = getParameter(request, "nickname");
    String group1 = getParameter(request, "group1");
    String group2 = getParameter(request, "group2");

    // Create a new entry in the roster that belongs to a certain groups
    if (user != null) {
        roster.createEntry(user, nickname, new String[] {group1, group2});
        response.sendRedirect("viewRoster.jsp");
        return;
    }
%>
<html>
<head>
<title>Add Contact</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="css/general.css" rel="stylesheet" type="text/css">
</head>
<body>
<table width="100%" border="0">
  <tr> 
    <td><span id="bigBlack">Add contact to roster</span></td>
    <td align="right"><a href="viewRoster.jsp"><img src="images/address_book.png" alt="View roster" border="0"></a></td>
  </tr>
  <tr> 
    <td colspan="2">&nbsp;</td>
  </tr>
  <tr> 
    <td colspan="2"><table width="100%" border="0">
        <tr> 
          <td width="20%">&nbsp;</td>
          <td width="60%"> <table cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
              <tr bgcolor="#AAAAAA"> 
                <td class=text id=bigWhite height=16 align="center"> <b>Contact 
                  Information</b> </td>
              </tr>
              <tr> 
                <td align="center"> <table width="100%" border="0">
                    <form method="post" action="addContact.jsp">
                      <tr> 
                        <td class=text id=black height=16>Username:</td>
                        <td><input type="text" name="user"> &nbsp;<span class=text id=black height=16>(e.g. 
                          johndoe@jabber.org)</span></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Nickname:</td>
                        <td><input type="text" name="nickname"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Group 1:</td>
                        <td><input type="text" name="group1" value="<%= (group1!=null)?group1:"" %>"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Group 2:</td>
                        <td><input type="text" name="group2"></td>
                      </tr>
                      <tr> 
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                      </tr>
                      <tr> 
                        <td colspan=2 align="center"> <input type="submit" value="Add Contact"></td>
                      </tr>
                    </form>
                  </table></td>
              </tr>
            </table></td>
          <td width="20%">&nbsp;</td>
        </tr>
      </table></td>
  </tr>
</table>
</body>
</html>
