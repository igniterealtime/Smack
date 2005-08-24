<%  // Set the content type of the this page to be CSS
    String contentType = "text/css";
    String chatAnnouncementColor = application.getInitParameter("chat.announcement-color");
    String chatOwnerLabelColor = application.getInitParameter("chat.owner-label-color");
    String chatParticipantLabelColor = application.getInitParameter("chat.participant-label-color");
    String chatTextColor = application.getInitParameter("chat.text-color");
    String errorTextColor = application.getInitParameter("error.text-color");
    String linkColor = application.getInitParameter("link.color");
    String linkHoverColor = application.getInitParameter("link.hover-color");
    String linkVisitedColor = application.getInitParameter("link.visited-color");
    String bodyBGColor = application.getInitParameter("body.background-color");
    String bodyTextColor = application.getInitParameter("body.text-color");
    String frameDividerColor = application.getInitParameter("frame.divider-color");
    String buttonColor = application.getInitParameter("button.color");
    String buttonTextColor = application.getInitParameter("button.text-color");
    String textFieldColor = application.getInitParameter("textfield.color");
    String textFieldTextColor = application.getInitParameter("textfield.text-color");
    response.setContentType(contentType);
    if (chatAnnouncementColor == null) {
        chatAnnouncementColor = "#009d00";
    }
    if (chatOwnerLabelColor == null) {
        chatOwnerLabelColor = "#aa0000";
    }
    if (chatParticipantLabelColor == null) {
        chatParticipantLabelColor = "#0000aa";
    }
    if (chatTextColor == null) {
        chatTextColor = "#434343";
    }
    if (errorTextColor == null) {
        errorTextColor = "#ff0000";
    }
    if (linkColor == null) {
        linkColor = "#045d30";
    }
    if (linkHoverColor == null) {
        linkHoverColor = "#350000";
    }
    if (linkVisitedColor == null) {
        linkVisitedColor = "#3b3757";
    }
    if (bodyBGColor == null) {
        bodyBGColor = "#ffffff";
    }
    if (bodyTextColor == null) {
        bodyTextColor = "#362f2d";
    }
    if (frameDividerColor == null) {
        frameDividerColor = "#83272b";
    }
    if (buttonColor == null) {
        buttonColor = "#d6dfdf";
    }
    if (buttonTextColor == null) {
        buttonTextColor = "#333333";
    }
    if (textFieldColor == null) {
        textFieldColor = "#f7f7fb";
    }
    if (textFieldTextColor == null) {
        textFieldTextColor = "#333333";
    }
%>

BODY, TD, TH    { font-family : Tahoma, Arial, Verdana, sans serif; font-size: 13px; }

H3              { font-size : 1.2em; }

.error-text     { color : <%= errorTextColor %>; }


/* default unvisited, visited and hover link presentation */
A:link			{ background: transparent; color: <%= linkColor %>;
					text-decoration: none; }
A:visited		{ background: transparent; color: <%= linkVisitedColor %>;
					text-decoration: none; }
A:hover			{ background: transparent; color: <%= linkHoverColor %>;
					text-decoration: underline; }

/**
 * site wide BODY style rule; the scrollbar stuff only works in IE for windows,
 *	but doesn't seem to hurt on other browsers..
 */
BODY.deffr		{ background-color: <%= bodyBGColor %>; color: <%= bodyTextColor %>;
				scrollbar-face-color: <%= bodyBGColor %>;
				scrollbar-shadow-color: <%= bodyTextColor %>;
				scrollbar-highlight-color: <%= bodyBGColor %>;
				scrollbar-darkshadow-color: <%= bodyBGColor %>;
				scrollbar-track-color: <%= bodyBGColor %>;
				scrollbar-arrow-color: <%= bodyTextColor %>; }


FRAME.bordered_left   { border-left: 3px solid <%= frameDividerColor %>; }


IMG.logo         { position: absolute; bottom: 12px; left: 10px; }

IMG.logout       { vertical-align: middle; }


INPUT.submit		{ background-color: <%= buttonColor %>; color: <%= buttonTextColor %>;
					font-size: 12px; font-family: Arial, Verdana, sans serif;
					border-style: ridge; margin: 1px 5px 1px 5px; }

INPUT.submit_right	{ background-color: <%= buttonColor %>; color: <%= buttonTextColor %>;
					font-size: 12px; font-family: Arial, Verdana, sans serif;
					border-style: ridge; margin: 1px 5px 1px 5px;
					position: absolute; right: 10px; }

INPUT.text		    { background-color: <%= textFieldColor %>; color: <%= textFieldTextColor %>;
					font: normal 12px Arial, Verdana, sans serif; height: 20px; width: 271px;
					border-style: groove; margin-left: 10px; }


SPAN.chat_text  { font: normal 11px Arial, Verdana, sans serif;
                  color: <%= chatTextColor %>; }

SPAN.chat_announcement  { font: italic 11px Arial, Verdana, sans serif;
                  color: <%= chatAnnouncementColor %>; }

SPAN.chat_owner  { font: bold 11px Arial, Verdana, sans serif;
                  color: <%= chatOwnerLabelColor %>; }

SPAN.chat_participant  { font: bold 11px Arial, Verdana, sans serif;
                  color: <%= chatParticipantLabelColor %>; }

SPAN.logout     { position: absolute; bottom: 12px; right: 15px; }


TEXTAREA        { color: <%= textFieldTextColor %>; font: normal 12px Arial, Verdana, sans serif;
                  width: 500px; height: 130px; }
