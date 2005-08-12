<%@	page import="javax.servlet.*" %>

<%	// Get error map as a request attribute:
    String logoFilename = application.getInitParameter("logoFilename");
    if (logoFilename == null) {
        logoFilename = "images/logo.gif";
    }
%>
<html>

	<head>
		<meta http-equiv="expires" content="0">

		<script>
			function updateButtonState (textAreaElement) {
				if (textAreaElement.value != '') {
					textAreaElement.form.send.disabled = false;
				} else {
					textAreaElement.form.send.disabled = true;
				}
			}

			function handleKeyEvent (event, textAreaElement) {
				var form = textAreaElement.form;
				var keyCode = event.keyCode;

				if (keyCode == null) {
					keyCode = event.which;
				}

				if (keyCode == 13) {
					submitForm(form);

					form.message.value = '';
				}

				updateButtonState(textAreaElement);
			}

			function submitForm (formElement) {
				var textAreaElement = formElement.message;
				var text = textAreaElement.value;
				var sForm = window.parent.frames['submitter'].document.chatform;

				sForm.message.value = text;
				sForm.submit();

				textAreaElement.value = '';

				updateButtonState(textAreaElement);

				return false;
			}

		</script>

	    <link rel="stylesheet" href="<%= request.getContextPath() %>/style_sheet.jsp"
                type="text/css">
	</head>

	<body class="deffr">
        <center>
		    <form name="chat" onsubmit="return submitForm(this);">
			    <textarea name="message"
				        onkeyup="handleKeyEvent(event, this);"
				        onchange="updateButtonState(this);"></textarea>
                <br>
			    <input type="submit" name="send" value="Send" class="submit_right" disabled>
		    </form>
        </center>

		<img src="<%= logoFilename %>" class="logo">
	</body>

</html>
