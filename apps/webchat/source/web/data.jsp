<%--
  -
  -
--%>

<%	// get the username of the current user
	String nickname = (String)session.getAttribute("messenger.servlet.nickname");
	if (nickname == null) {
		nickname = "";
	}
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
<head>
	<title>Chat Data</title>
    <script language="JavaScript" type="text/javascript">
    var currUsername = "<%= nickname %>";
    var data = new Array();
    </script>
</head>

<body></body>

</html>
