<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

	    <title>Web Chat Session</title>
	    <script language="JavaScript" type="text/javascript">

	        function launchWin() {
		        var newWin = window.open("frame_master.jsp", "chatWin",
			     "location=no,status=no,toolbar=no,personalbar=no,menubar=no,width=650,height=430");
	        }

	    </script>

	    <link rel="stylesheet" href="<%= request.getContextPath() %>/style_sheet.jsp"
                type="text/css">
    </head>

    <body class="deffr" onload="launchWin();">

        <h3>Chat Session Options</h3>

            Your chat session should have already started. If for some reason it did
            not, click <a href="#" onclick="launchWin(); return false;">this link</a>
            to start your chat session.

        <br><br>

        Other options:

        <ul>
	        <li><a href="email" onclick="alert('Coming soon'); return false;">Email Transcript</a>
	        <li><a href="index.jsp">Return to the login page.</a>
        </ul>

    </body>
</html>
