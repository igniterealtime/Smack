<html>

	<head>
		<meta http-equiv="expires" content="0">

        <script>

			function verifyLogout () {
				if (confirm("Are you sure you'd like to logout")) {
                    // hacky solution to avoid logging out twice due to parent frame's onUnload
                    try {
                        document.logout.command.value = "silence";
                    } catch (e) { }

					window.parent.close();

					return true;
				}

				return false;
			}

        </script>

	    <link rel="stylesheet" href="<%= request.getContextPath() %>/style_sheet.jsp"
                type="text/css">
	</head>

	<body class="deffr">
	    <center>In the room:</center>
	    <br>
	    <hr width=67%>

        <div id="par__list"> </div>

        <form name="logout" action="<%= request.getContextPath() %>/ChatServlet" method="post">
		    <input type="hidden" name="command" value="logout">
	    </form>

        <span class="logout">
            <a href="<%= request.getContextPath() %>/ChatServlet?command=logout"
                    onclick="return verifyLogout();" >
		        <img src="images/logout-16x16.gif" border="0" class="logout"> Logout</a></span>
	</body>

</html>
