<%--
  -
  -
--%>

<%@	page import="java.util.*" %>

<%	// Get error map as a request attribute:
	Map errors = (Map)request.getAttribute("messenger.servlet.errors");
	if (errors == null) { errors = new HashMap(); }
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
	<head>
		<title>Create an account</title>

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
                                <h3>Jive Account Creation</h3>

	                            <%	if (errors.get("general") != null) { %>
                                    <p>
                                        <span class="error-text">
		                                    Error creating account. <%= errors.get("general") %>
		                                </span>
		                            </p>
                                    <br>
                                <%	} %>
                            </td>
                        </tr>
                        <tr>
                            <td align="center">
	                            <form action="<%= request.getContextPath() %>/ChatServlet"
                                        method="post" name="createform">
	                            <input type="hidden" name="command" value="create_account">

	                            <table cellpadding="2" cellspacing="0" border="0">
	                                <tr>
		                                <td>Desired username:</td>
		                                <td>
			                                <input type="text" size="40" name="username"
                                                    class="text">
                                            <% if (errors.get("empty_username") != null) { %>
                                                <span class="error-text"><br>
				                                    Please enter a username.
				                                </span>
			                                <%	} %>
		                                </td>
	                                </tr>
	                                <tr>
                                        <td>Desired password:</td>
                                        <td>
    	                                    <input type="password" size="40" name="password"
                                                     class="text">
			                                <% if (errors.get("empty_password") != null) { %>
                                                <span class="error-text"><br>
				                                    Please enter a password.
				                                </span>
			                                <%	} %>
			                                <% if (errors.get("mismatch_password") != null) { %>
                                				<span class="error-text"><br>
				                                    Your passwords did not match.
			                                    </span>
			                                <%	} %>
		                                </td>
	                                </tr>
	                                <tr>
		                                <td>Retype your password:</td>
		                                <td>
		                                    <input type="password" size="40" name="password_zwei"
                                                class="text">
			                                <% if (errors.get("empty_password_two") != null) { %>
                                                <span class="error-text"><br>
                                                    You must retype your password.
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
                                <input type="submit" name="" value="Create account"
                                    class="submit">
                            	</form>
                            </td>
                        </tr>
                        <tr>
                            <td align="center">
                                <br><a href="index.jsp">Click here to return to the login page.</a>
                            </td>
                        </tr>
					</table>
				</td>
			</tr>
		</table>

	    <script language="JavaScript" type="text/javascript">
	        document.createform.username.focus();
	    </script>
	</body>
</html>
