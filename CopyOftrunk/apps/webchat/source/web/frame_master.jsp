<html>
	<head>
		<title><%= request.getSession().getAttribute("messenger.servlet.room") %>
                 - Jive Web Chat Client</title>

        <script>

            function frameSetLoaded () {
                window.frames['poller'].location.href
                        = "<%= request.getContextPath() %>/ChatServlet?command=read";
            }

            function attemptLogout () {
                window.frames['participants'].document.logout.submit();
            }

        </script>

	    <link rel="stylesheet" href="<%= request.getContextPath() %>/style_sheet.jsp"
                type="text/css">
	</head>

	<frameset cols="*, 125" border="0" frameborder="0" framespacing="0"
        onLoad="frameSetLoaded();" onUnload="attemptLogout();">
	    <frameset rows="0, 200, *, 0" border="0" frameborder="0" framespacing="0">
    		<frame name="submitter" src="chat-hiddenform.jsp" frameborder="0">
    		<frame name="yak" src="transcript_frame.html" frameborder="0">
    		<frame name="input" src="input_frame.jsp" frameborder="0">
    		<frame name="poller" src="" frameborder="0">
	    </frameset>
        <frame name="participants" src="participants_frame.jsp" class="bordered_left">
	</frameset>
</html>
