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
	<title>Chat</title>
	<script language="JavaScript" type="text/javascript">
	function submitForm(el) {
		el.form.submit();
	}
	</script>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/style.css" type="text/css">
</head>

<body>

<h3>Jive Chat Login</h3>

<%	if (errors.get("general") != null) { %>

	<p class="error-text">
	Error logging in. Make sure your username and password is correct.
	</p>

<%	} %>

<form action="<%= request.getContextPath() %>/servlet/ChatServlet" method="post" name="loginform">
<input type="hidden" name="command" value="login">
<input type="hidden" name="garbage" value="<%= Math.random() %>">

<table cellpadding="2" cellspacing="0" border="0">
<tr>
	<td>Username:</td>
	<td>
		<input type="text" size="40" name="username">
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
		<input type="password" size="40" name="password">
		<%	if (errors.get("password") != null) { %>
		
			<span class="error-text"><br>
			Please enter a valid password.
			</span>
		
		<%	} %>
	</td>
</tr>
<tr>
	<td>Nickname:</td>
	<td>
		<input type="text" size="40" name="nickname">
		<%	if (errors.get("nickname") != null) { %>
		
			<span class="error-text"><br>
			Please enter a valid nickname.
			</span>
		
		<%	} %>
	</td>
</tr>
<tr>
	<td>Room:</td>
	<td>
		<input type="text" size="40" name="room" value="test@chat.jivesoftware.com">
		<%	if (errors.get("room") != null) { %>
		
			<span class="error-text"><br>
			Please enter a valid room.
			</span>
		
		<%	} %>
	</td>
</tr>
</table>

<br>

<input type="submit" name="" value="Start Chat">
</form>

<script language="JavaScript" type="text/javascript">
document.loginform.username.focus();
</script>

</body>
</html>
