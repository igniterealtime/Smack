function addChatText (someText, isAnnouncement) {
    var yakDiv = window.parent.frames['yak'].document.getElementById('ytext');
    var children = yakDiv.childNodes.length;
    var appendFailed = false;
    var spanElement = document.createElement("span");

    if (! isAnnouncement) {
        spanElement.setAttribute("class", "chat_text");
    } else {
        spanElement.setAttribute("class", "chat_announcement");
    }
    // it's easier to dump the possibily html-containing text into the innerHTML
    //  of the span element than deciphering and building sub-elements.
    spanElement.innerHTML = someText;

    try {
        // various versions of IE crash out on this, and safari
        yakDiv.appendChild(spanElement);
    } catch (exception) {
        appendFailed = true;
    }

    if (! appendFailed) {
        // really make sure the browser appended
        appendFailed = (children == yakDiv.childNodes.length);
    }

    if (appendFailed) {
        // try this, the only way left
        var inn = yakDiv.innerHTML;

        inn += "<span class=\"";
        inn += (isAnnouncement ? "chat_announcement\">" : "chat_text\">");
        inn += someText + "</span><br>";

        yakDiv.innerHTML = inn;
    } else {
        yakDiv.appendChild(document.createElement("br"));
    }

    scrollYakToEnd();
}

function addUserName (userName) {
    var yakDiv = window.parent.frames['yak'].document.getElementById('ytext');
    var children = yakDiv.childNodes.length;
    var appendFailed = false;
    var spanElement = document.createElement("span");
    var userIsClientOwner = false;
    var announcement = false;

    if (userName == "") {
        announcement = true;

        spanElement.setAttribute("class", "chat_announcement");

	    userName = "room announcement";
    } else if (userName == nickname) {
	    userIsClientOwner = true;

        spanElement.setAttribute("class", "chat_owner");
    } else {
        spanElement.setAttribute("class", "chat_participant");
    }

    try {
        spanElement.appendChild(document.createTextNode(userName + ": "));

        // various versions of IE crash out on this, and safari
        yakDiv.appendChild(spanElement);
    } catch (exception) {
        appendFailed = true;
    }

    if (! appendFailed) {
        // really make sure the browser appended
        appendFailed = (children == yakDiv.childNodes.length);
    }

    if (appendFailed) {
        // try this, the only way left
        var inn = yakDiv.innerHTML

        inn += "<span class=\"";

	if (announcement) {
	    inn += "chat_announcement"
	} else if (userIsClientOwner) {
            inn += "chat_owner";
        } else {
            inn += "chat_participant";
        }

        inn += "\">" + userName + ": </span>";

        yakDiv.innerHTML = inn;
    }
}

function scrollYakToEnd () {
    var endDiv = window.parent.frames['yak'].document.getElementById('enddiv');

    window.parent.frames['yak'].window.scrollTo(0, endDiv.offsetTop);
}

function userJoined (username) {
    var parentDIV = window.parent.frames['participants'].document.getElementById('par__list');
    var children = parentDIV.childNodes.length;
    var appendFailed = false;
    var divElement = document.createElement("div");

    divElement.setAttribute("id", username);

    try {
        divElement.appendChild(document.createTextNode(username));
        divElement.appendChild(document.createElement("br"));

        parentDIV.appendChild(divElement);
    } catch (exception) {
        appendFailed = true;
    }

    if (! appendFailed) {
        // really make sure the browser appended
        appendFailed = (children == parentDIV.childNodes.length);
    }

    if (appendFailed) {
        // try this, the only way left
        var inn = parentDIV.innerHTML;

        inn += "<div id=\"" + username + "\"> &middot; " + username + "<br></div>";

        parentDIV.innerHTML = inn;
    }
}

function userDeparted (username) {
    var partDoc = window.parent.frames['participants'].document;
    var parentDIV = partDoc.getElementById('par__list');
    var userDIV = partDoc.getElementById(username);
    var children = parentDIV.childNodes.length;
    var removeFailed = false;

    // MAY RETURN THIS BLOCK
    if (userDIV == null) {
        return;
    }

    try {
        parentDIV.removeChild(userDIV);
    } catch (exception) {
        removeFailed = true;
    }

    if (! removeFailed) {
        // really make sure the browser appended
        removeFailed = (children == parentDIV.childNodes.length);
    }

    if (removeFailed) {
        // try this, the only way left
        var inn = parentDIV.innerHTML;
        var openingTag = "<div id=\"" + username + "\">";
        var index = inn.toLowerCase().indexOf(openingTag);
        var patchedHTML = inn.substring(0, index);
        var secondIndex = openingTag.length + username.length + 13;

        patchedHTML += inn.substring(secondIndex, (inn.length));

        parentDIV.innerHTML = inn;
    }
}

function writeDate () {
    var msg = "This frame loaded at: ";
    var now = new Date();

    msg += now + "<br><hr>";

    document.write(msg);
}
