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
<%@ page import="org.jivesoftware.smack.*"%>
<%@ include file="global.jsp" %>
<%
    // If we already got a connection then proceed to view the roster
    TCPConnection conn = (XMPPConnection) session.getAttribute("connection");
    if (conn != null && conn.isConnected()) {
        response.sendRedirect("viewRoster.jsp");
        return;
    }

    // Get parameters
    String host        = getParameter(request, "host");
    String port        = getParameter(request, "port");
    String ssl         = getParameter(request, "ssl");
    String debug       = getParameter(request, "debug");
    String username    = getParameter(request, "username");
    String password    = getParameter(request, "password");
    String resource    = getParameter(request, "resource");
    String error       = getParameter(request, "error");

    // Try to connect to the server
    if (error == null && host != null && port != null) {
        TCPConnection.DEBUG_ENABLED = "Yes".equals(debug);
        try {
            if ("No".equals(ssl)) {
                conn = new TCPConnection(host);
            }
            else {
                conn = new TCPConnection(host);
            }
            conn.connect();
            // Add listener for messages (offline messages will be listen here)
            
            // Set the roster subscription mode to use 
            
            // Login to the server
            conn.login(username, password, resource);
            session.setAttribute("connection", conn);
        }
        catch (Exception e) {
            error = e.getMessage();
            // Replace any char : because otherwise the URL will get corrupted
            error = error.replace(':', '-');
            response.sendRedirect("login.jsp?host="+host+"&port="+port+"&ssl="+ssl+"&error="+error);
            return;
        }
        // Redirect to the next page
        response.sendRedirect("viewRoster.jsp");
        return;
    }
%>
<html>
<head>
<title>Login</title>
<link href="css/general.css" rel="stylesheet" type="text/css">
</head>
<body>
<table width="100%" border="0">
  <tr> 
    <td><span id="bigBlack">Smack Demo Application</span></td>
  </tr>
  <tr> 
    <td align="center"> 
      <% if (error != null) { %>
      <p><font color="#FF0000"><%= error %></font></p>
      <%} else {%>
      &nbsp; 
      <%}%>
    </td>
  </tr>
  <tr> 
    <td><table width="100%" border="0">
        <tr> 
          <td width="20%">&nbsp;</td>
          <td width="60%"> <table cellSpacing=0 borderColorDark=#E0E0E0 cellPadding=0 width=100% align=center borderColorLight=#000000 border=1>
              <tr bgcolor="#AAAAAA"> 
                <td class=text id=bigWhite height=16 align="center"> <b>Login 
                  Information</b> </td>
              </tr>
              <tr> 
                <td><form action="login.jsp" method="post">
                 <table width=100% border=0>
                    
                      <tr> 
                        <td class=text id=black height=16>Host:</td>
                        <td><input type="text" name="host" size="30" maxlength="50" value="<%= (host!=null)?host:"" %>"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Port:</td>
                        <td><input type="text" name="port" size="5" maxlength="10" value="<%= (port!=null)?port:"5222" %>"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Use SSL:</td>
                        <td><select size="1" name="ssl">
                            <option <%= ("No".equals(ssl))?"selected":""%>>No</option>
                            <option <%= ("Yes".equals(ssl))?"selected":""%>>Yes</option>
                          </select></td>
                      </tr>
                      <tr> 
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Debug Connection:</td>
                        <td><select size="1" name="debug">
                            <option <%= ("No".equals(debug))?"selected":""%>>No</option>
                            <option <%= ("Yes".equals(debug))?"selected":""%>>Yes</option>
                          </select></td>
                      </tr>
                      <tr> 
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Username:</td>
                        <td><input type="text" name="username" size="30" maxlength="50" value="<%= (username!=null)?username:"" %>"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Password:</td>
                        <td><input type="password" name="password" size="30" maxlength="50" value="<%= (password!=null)?password:"" %>"></td>
                      </tr>
                      <tr> 
                        <td class=text id=black height=16>Resource:</td>
                        <td><input type="text" name="resource" size="30" maxlength="50" value="<%= (resource!=null)?resource:"" %>"></td>
                      </tr>
                      <tr> 
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                      </tr>
                      <tr align="center"> 
                        <td colspan="2"> <input type="submit" value="Login"> </td>
                      </tr>
                    
                  </table></form></td>
              </tr>
            </table></td>
          <td width="20%">&nbsp;</td>
        </tr>
      </table></td>
  </tr>
</table>
</body>
</html>
