<%--
  -
  -
--%>

<%@	page import="java.util.*" %>

<%	// Get error map as a request attribute:
	Map errors = (Map)request.getAttribute("messenger.servlet.errors");
    boolean allowAnonymous = true;
    boolean allowAccountCreate = true;
    boolean allowLogin = true;
    String param = null;
	if (errors == null) { errors = new HashMap(); }
    param = application.getInitParameter("allowAnonymous");
    if ((param != null) && (param.equalsIgnoreCase("false"))) {
        allowAnonymous = false;
    }
    param = application.getInitParameter("allowAccountCreation");
    if ((param != null) && (param.equalsIgnoreCase("false"))) {
        allowAccountCreate = false;
    }
    param = application.getInitParameter("allowLogin");
    if ((param != null) && (param.equalsIgnoreCase("false"))) {
        allowLogin = false;
    }
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
    <head>
	    <title>Jive Web Chat Client Login</title>
        
	    <script language="JavaScript" type="text/javascript">
	        function submitForm (el) {
	    	    el.form.submit();
	        }

            function anonClick () {
                document.loginform.command.value = "anon_login";

                return true;
            }
	    </script>

	    <link rel="stylesheet" href="<%= request.getContextPath() %>/style_sheet.jsp"
                type="text/css">
    </head>

    <body class="deffr">

		<table width="100%" height="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td align="center" valign="middle">
					<table cellpadding="0" cellspacing="0" border="0">
						<tr>
                            <td>
                                <h3>Welcome to the Jive Web Chat Client - Please Login</h3>
                                    <%	if (errors.get("general") != null) { %>
                                    	<p class="error-text">
                                            Error logging in. Make sure your username and
                                                password are correct. <%= errors.get("general") %>
	                                    </p>
                                    <%	} %>
                            </td>
                        </tr>
                        <tr>
                            <td align="center">
                                <form action="<%= request.getContextPath() %>/ChatServlet"
                                    method="post" name="loginform">
                                <input type="hidden" name="command" value="login">

                                <table cellpadding="2" cellspacing="0" border="0">
                                    <% if (allowLogin) { %>
                                        <tr>
	                                        <td>Username:</td>
	                                        <td>
		                                        <input type="text" size="40" name="username"
                                                    class="text">
		                                        <%	if (errors.get("username") != null) { %>
                                    			    <span class="error-text"><br>
			                                            Please enter a valid username.
			                                        </span>
                                    		    <%	} %>
	                                        </td>
                                        </tr>
                                        <tr>
	                                        <td>Password:</td>
	                                        <td>
		                                        <input type="password" size="40" name="password"
                                                    class="text">
		                                        <%	if (errors.get("password") != null) { %>
                                    			    <span class="error-text"><br>
			                                            Please enter a valid password.
			                                        </span>
                                    		    <%	} %>
	                                        </td>
                                        </tr>
                                    <%  } %>
                                    <tr>
	                                    <td>Nickname:</td>
	                                    <td>
		                                    <input type="text" size="40" name="nickname"
                                                    class="text">
		                                    <%	if (errors.get("nickname") != null) { %>
                                  			    <span class="error-text"><br>
			                                        Please enter a nickname.
			                                    </span>
		                                    <%	} %>
	                                    </td>
                                    </tr>
                                    <tr>
	                                    <td>Room:</td>
	                                    <td>
		                                    <input type="text" size="40" name="room"
                                                    value="test@chat.jivesoftware.com" class="text">
		                                    <%	if (errors.get("room") != null) { %>
                                    			<span class="error-text"><br>
			                                        Please enter a valid room.
			                                    </span>
		                                    <%	} %>
	                                    </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <td align="center">
                                <br>
                                <% if (allowLogin) { %>
                                    <input type="submit" name="" value="Login and Chat"
                                            class="submit">
                                <%	} %>
                                <% if (allowAnonymous) { %>
                                    <input type="submit" name="" value="Anonymously Chat"
                                            onClick="return anonClick();" class="submit">
                                <%	} %>
                                </form>
                            </td>
						</tr>
                        <% if (allowAccountCreate) { %>
                            <tr>
                                <td align="center">
                                    <br>Don't have an account and would like to create one?
                                        <a href="account_creation.jsp">Click here.</a>
                                </td>
                            </tr>
                        <%  } %>
					</table>
				</td>
			</tr>
		</table>

        <script language="JavaScript" type="text/javascript">
            <% if (allowLogin) { %>
                document.loginform.username.focus();
            <% } else { %>
                document.loginform.nickname.focus();
            <%  } %>
        </script>

    </body>
</html>
