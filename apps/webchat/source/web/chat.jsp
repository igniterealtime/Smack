<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Chat Session</title>
    <script language="JavaScript" type="text/javascript">
    function handleLogoff() {
        if (confirm('Are you sure you want to end this chat session and close this window?')) {
            window.close();
        }
        return false;
    }
    </script>
</head>

<frameset rows="25,30,*,95,0,0,0" border="0" frameborder="0" framespacing="0">
    <frame name="main" src="menu.html"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
    <frame name="main" src="logo.html"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
    <frame name="main" src="iframe.html"
		marginwidth="0" marginheight="0" scrolling="yes" frameborder="0">
    <frame name="form" src="chat-form.html"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
    <frame name="hiddenform" src="chat-hiddenform.jsp"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
	<frame name="data" src="data.jsp"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
    <frame name="server" src="<%= request.getContextPath() %>/servlet/ChatServlet?command=read"
		marginwidth="0" marginheight="0" scrolling="no" frameborder="0">
</frameset>

</html>