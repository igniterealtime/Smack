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
    String action = getParameter(request, "action");
    String user = getParameter(request, "user");
    String fromGroup = getParameter(request, "fromGroup");
    String toGroup = getParameter(request, "toGroup");

    // Move the entry from the existing group to a new group
    if ("move".equals(action)) {
        RosterEntry entry = roster.getEntry(user);
        // Remove the entry from the existing group
        RosterGroup rosterGroup = roster.getGroup(fromGroup);
        rosterGroup.removeEntry(entry);
        // Get the new group or create it if it doesn't exist
        rosterGroup = roster.getGroup(toGroup);
        if (rosterGroup == null) {
            rosterGroup = roster.createGroup(toGroup);
        }
        // Add the new entry to the group
        rosterGroup.addEntry(entry);
        response.sendRedirect("viewRoster.jsp");
        return;
    }

    // Add the entry to a new group
    if ("add".equals(action)) {
        RosterEntry entry = roster.getEntry(user);
        // Get the new group or create it if it doesn't exist
        RosterGroup rosterGroup = roster.getGroup(toGroup);
        if (rosterGroup == null) {
            rosterGroup = roster.createGroup(toGroup);
        }
        // Add the new entry to the group
        rosterGroup.addEntry(entry);
        response.sendRedirect("viewRoster.jsp");
        return;
    }

    // Delete the entry from a group
    if ("delete".equals(action)) {
        RosterEntry entry = roster.getEntry(user);
        RosterGroup rosterGroup = roster.getGroup(fromGroup);
        rosterGroup.removeEntry(entry);
        response.sendRedirect("viewRoster.jsp");
        return;
    }
%>
<html>
<head>
<title>Groups Management</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="css/general.css" rel="stylesheet" type="text/css">
</head>
<body>
<table width="100%" border="0">
  <tr> 
    <td><span id="bigBlack">Groups for entry: <%= user%></span></td>
    <td align="right"><a href="viewRoster.jsp"><img src="images/address_book.png" alt="View roster" border="0"></a></td>
  </tr>
  <tr> 
    <td colspan="2">&nbsp;</td>
  </tr><tr><td colspan="2">
  <table width="100%" border="0"><tr>
    <td valign="top" width="48%"><table cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
        <tr bgcolor="#AAAAAA"> 
          <td class=text id=bigWhite height=16 align="center"> <b>Add to new group</b> 
          </td>
        </tr>
        <tr> 
          <td> <table width="100%" border="0">
              <form method="post" action="moveContact.jsp">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="user" value="<%= user%>">
                <tr> 
                  <td width="40%" height=16 align="center" class=text>Group:</td>
                  <td width="20%">&nbsp;</td>
                  <td width="40%" align="center"><input type="text" name="toGroup"></td>
                </tr>
                <tr> 
                  <td colspan=3 align="center"> <input type="submit" value="Add"></td>
                </tr>
              </form>
            </table></td>
        </tr>
      </table></td>
    <td width="4%">&nbsp;</td><td valign="top" width="48%">
    <table cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
      <tr bgcolor="#AAAAAA"> 
        <td colspan="3" class=text id=bigWhite height=16 align="center"> <b>Move 
          to new group</b> </td>
      </tr>
      <tr vAlign=center align=middle bgcolor="#AAAAAA"> 
        <td align="center" width="40%" class=text id=white height=16>From</td>
        <td width="20%">&nbsp;</td>
        <td align="center" width="40%" class=text id=white height=16>To</td>
      </tr>
      <% if (fromGroup != null) { %>
      <form method="post" action="moveContact.jsp">
        <input type="hidden" name="action" value="move">
        <input type="hidden" name="user" value="<%= user%>">
        <input type="hidden" name="fromGroup" value="<%= fromGroup%>">
        <tr> 
          <td colspan="3"> <table width="100%" border="0">
              <tr> 
                <td width="40%" align="center" valign="middle" class=text height=16><%=fromGroup%></td>
                <td width="20%" align="center" valign="middle"><img src="images/nav_right_blue.png" alt="Move contact to group" border="0"></td>
                <td width="40%" align="center" valign="middle"> <input type="text" name="toGroup"> 
              </tr>
              <tr> 
                <td colspan="3" align="center"> <input type="submit" value="Move"> 
              </tr>
            </table></td>
        </tr>
      </form>
      <%  } else { %>
      <tr> 
        <td colspan="3" align="center" class=text id=black height=16>Entry does 
          not belong to a group (unfiled entry) </td>
      </tr>
      <%  } %>
    </table></td></tr>
    <tr> 
      <td colspan="3">&nbsp;</td>
    </tr>
    <tr> 
      <td colspan="3" align="center"> <table cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=50% align=center borderColorLight=#000000 border=1>
          <tr bgcolor="#AAAAAA"> 
            <td colspan="2" class=text id=bigWhite height=16 align="center"> <b>Remove 
              from groups</b> </td>
          </tr>
          <% 
                    RosterEntry entry = roster.getEntry(user);
                    RosterGroup group;
                    for (Iterator<RosterGroup> it=entry.getGroups().iterator(); it.hasNext();) { 
                        group = it.next();%>
          <tr> 
            <td align="center" width="80%" class=text height=16><%=group.getName()%></td>
            <td valign="middle" align="center" width="20%"><a href="moveContact.jsp?action=delete&user=<%=user%>&fromGroup=<%=group.getName()%>"><img src="images/garbage.png" alt="Remove contact from the group" border="0"></a></td>
          </tr>
          <% } %>
        </table></td>
    </tr>
  </table></td></tr>
</table>
</body>
</html>
