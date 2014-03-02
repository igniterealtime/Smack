<%--
  - $$RCSfile$$
  - $$Revision$$
  - $$Date$$
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
    String groupName = getParameter(request, "group");

    // Remove the selected user from the roster (and all the groups)
    if ("rosterDelete".equals(action)) {
        RosterEntry entry = roster.getEntry(user);
        roster.removeEntry(entry);
        response.sendRedirect("viewRoster.jsp");
        return;
    }

    // Remove the selected user from the selected group
    if ("groupDelete".equals(action)) {
        RosterEntry entry = roster.getEntry(user);
        RosterGroup rosterGroup = roster.getGroup(groupName);
        rosterGroup.removeEntry(entry);
        response.sendRedirect("viewRoster.jsp");
        return;
    }

    // Close the connection to the XMPP server
    if ("close".equals(action)) {
        conn.disconnect();
        session.invalidate();
        response.sendRedirect("login.jsp");
        return;
    }
%>
<html>
<head>
<title>Viewing roster</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link href="css/general.css" rel="stylesheet" type="text/css">
</head>
<body>
<table width="100%" border="0">
  <tr> 
    <td width="50%"><span id="bigBlack"><%= StringUtils.parseName(conn.getUser())%>'s roster</span></td>
    <td width="50%"><table width="100%" border="0">
        <tr> 
          <td>&nbsp;</td>
          <td width="24"><a href="viewRoster.jsp"><img src="images/refresh.png" alt="Refresh roster" border="0"></a></td>
          <td width="24"><a href="viewRoster.jsp?action=close"><img src="images/plug_delete.png" alt="Close connection" border="0"></a></td>
        </tr>
      </table></td>
  </tr>
  <tr> 
    <td colspan="2"><table width="100%" border="0">
        <tr> 
          <td width="49%" valign="top"> <TABLE cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
              <tr bgcolor="#AAAAAA"> 
                <td class=text id=bigWhite height=16 align="center"> <b>Roster entries</b> </td>
              </tr>
              <tr> 
                <td> <TABLE cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
                    <% for (Iterator<RosterGroup> groups = roster.getGroups().iterator(); groups.hasNext();) {
                    RosterGroup group = groups.next();%>
                    <tr> 
                      <td bgcolor="#AAAAAA" colspan="6" class=text id=white height=16>Group: 
                        <%= group.getName()%></td>
                    </tr>
                    <TR vAlign=center align=middle bgcolor="#AAAAAA"> 
                      <TD class=text id=white height=16>User</TD>
                      <TD class=text id=white height=16>Name</TD>
                      <TD class=text id=white height=16>Subscription</TD>
                      <TD colspan="3" class=text id=white height=16><a href="addContact.jsp?group1=<%=group.getName()%>"><img src="images/businessman_add.png" alt="Add contact to group" border="0"></a></TD>
                    </TR>
                    <% for (Iterator<RosterEntry> it = group.getEntries().iterator(); it.hasNext();) { 
                        RosterEntry entry = it.next();%>
                    <TR vAlign=center align=middle bgColor=#ffffff> 
                      <TD class=text height=16><%= entry.getUser()%></TD>
                      <TD class=text height=16><%= entry.getName()%></TD>
                      <TD class=text height=16><%= entry.getType()%></TD>
                      <TD class=text height=16><a href="moveContact.jsp?user=<%=entry.getUser()%>&fromGroup=<%=group.getName()%>"><img src="images/businessmen.png" alt="Groups management" border="0"></a></TD>
                      <TD class=text height=16><a href="viewRoster.jsp?action=groupDelete&user=<%=entry.getUser()%>&group=<%=group.getName()%>"><img src="images/businessman_delete.png" alt="Remove contact from the group" border="0"></a></TD>
                      <TD class=text height=16><a href="viewRoster.jsp?action=rosterDelete&user=<%=entry.getUser()%>"><img src="images/garbage.png" alt="Remove contact from the roster" border="0"></a></TD>
                    </TR>
                    <% }%>
                    <% }%>
                    <tr> 
                      <td bgcolor="#AAAAAA" colspan="6" class=text id=white height=16>Unfiled 
                        entries</td>
                    </tr>
                    <TR vAlign=center align=middle bgcolor="#AAAAAA"> 
                      <TD class=text id=white height=16>User</TD>
                      <TD class=text id=white height=16>Name</TD>
                      <TD class=text id=white height=16>Subscription</TD>
                      <TD colspan="3" class=text id=white height=16><a href="addContact.jsp"><img src="images/businessman_add.png" alt="Add contact" border="0"></a></TD>
                    </TR>
                    <% for (Iterator<RosterEntry> it = roster.getUnfiledEntries().iterator(); it.hasNext();) { 
                RosterEntry entry = it.next();%>
                    <tr vAlign=center align=middle bgColor=#ffffff> 
                      <td class=text height=16><%= entry.getUser()%></td>
                      <td class=text height=16><%= entry.getName()%></td>
                      <td class=text height=16><%= entry.getType()%></td>
                      <TD class=text height=16><a href="moveContact.jsp?user=<%=entry.getUser()%>"><img src="images/businessmen.png" alt="Groups management" border="0"></a></td>
                      <TD class=text height=16>&nbsp;</TD>
                      <TD class=text height=16><a href="viewRoster.jsp?action=rosterDelete&user=<%=entry.getUser()%>"><img src="images/garbage.png" alt="Remove contact from roster" border="0"></a></td>
                    </tr>
                    <% }%>
                  </table></td>
              </tr>
            </table></td>
          <td width="2%" valign="top">&nbsp;</td>
          <td width="49%" valign="top"> <TABLE cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
              <tr bgcolor="#AAAAAA"> 
                <td class=text id=bigWhite height=16 align="center"> <b>Presences</b> 
                </td>
              </tr>
              <tr> 
                <td> <TABLE cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
                    <tr> 
                      <td bgcolor="#AAAAAA" colspan="5" class=text id=white height=16>Roster´s presences</td>
                    </tr>
                    <TR vAlign=center align=middle bgcolor="#AAAAAA"> 
                      <TD class=text id=white height=16>User</TD>
                      <TD class=text id=white height=16>Mode</TD>
                      <TD class=text id=white height=16>Type</TD>
                      <TD class=text id=white height=16>&nbsp;</TD>
                    </TR>
                    <% for (Iterator<RosterEntry> entries = roster.getEntries().iterator(); entries.hasNext();) {
                    RosterEntry entry = entries.next();
                    Iterator presences = roster.getPresences(entry.getUser());
                    if (presences != null) {
                        while (presences.hasNext()) {
                            Presence presence = (Presence)presences.next(); %>
                    <TR vAlign=center align=middle bgColor=#ffffff> 
                      <TD class=text height=16><%= presence.getFrom()%></TD>
                      <TD class=text height=16><%= presence.getMode()%></TD>
                      <TD class=text height=16><%= presence.getType()%></TD>
                      <TD class=text height=16><a href="chat.jsp?user=<%=presence.getFrom()%>"><img src="images/messages.png" alt="Chat" border="0"></a></a></TD>
                    </TR>
                    <% }%>
                    <% }%>
                    <% }%>
                    <tr> 
                      <td bgcolor="#AAAAAA" colspan="5" class=text id=white height=16>My other resources</td>
                    </tr>
                    <TR vAlign=center align=middle bgcolor="#AAAAAA"> 
                      <TD class=text id=white height=16>User</TD>
                      <TD class=text id=white height=16>Mode</TD>
                      <TD class=text id=white height=16>Type</TD>
                      <TD class=text id=white height=16>&nbsp;</TD>
                    </TR>
                    <%  // Show other presences of the current user
                Iterator presences = roster.getPresences(conn.getUser());
                if (presences != null) {
                    while (presences.hasNext()) {
                        Presence presence = (Presence)presences.next(); %>
                    <tr vAlign=center align=middle bgColor=#ffffff> 
                      <TD class=text height=16><%= presence.getFrom()%></TD>
                      <TD class=text height=16><%= presence.getMode()%></TD>
                      <TD class=text height=16><%= presence.getType()%></TD>
                      <TD class=text height=16><a href="chat.jsp?user=<%=presence.getFrom()%>"><img src="images/messages.png" alt="Chat" border="0"></a></a></TD>
                    </tr>
                    <% }%>
                    <% }%>
                  </table></td>
              </tr>
            </table></tr>
      </table></td>
  </tr>
</table>
</body>
</html>
