<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Chat Session</title>
	<script language="JavaScript" type="text/javascript">
	function launchWin() {
		var newWin = window.open("chat.jsp","chatWin",
			"location=no,status=no,toolbar=no,personalbar=no,menubar=no,width=600,height=400");
	}
	</script>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/style.css" type="text/css">
</head>

<body onload="launchWin();">

<h3>Chat Session Options</h3>

You chat session should have already started. If for some reason it did
not, click <a href="#" onclick="launchWin();return false;">this link</a>
to start your chat session.

<br><br>

Other options:

<ul>
	<li><a href="email">Email Transcript</a>
	<li><a href="something">Something Else</a>
</ul>

</body>

</html>
