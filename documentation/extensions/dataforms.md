Data Forms
==========

Allows to exchange structured data between users and applications for common
tasks such as registration and searching using Forms.

  * Create a Form to fill out
  * Answer a Form

**XEP related:** [XEP-4](http://www.xmpp.org/extensions/xep-0004.html)

Create a Form to fill out
-------------------------

**Description**

An XMPP entity may need to gather data from another XMPP entity. Therefore,
the data-gathering entity will need to create a new Form, specify the fields
that will conform the Form and finally send the Form to the data-providing
entity.

**Usage**

In order to create a Form to fill out use the _**Form**_'s constructor passing
the constant **DataForm.type.form** as the parameter. The next step is to create
the form fields and add them to the form. In order to create and customize a
_**FormField**_ use the _**FormField**_'s constructor specifying the variable
name of the field as the parameter. Then use **setType(String type)** to set
the field's type (e.g. FormField.type.hidden, FormField.type.text_single).
Once we have the _**Form**_ instance and the _**FormFields**_ the last step is
to send **addField(FormField field)** for each field that we want to add to
the form.

Once the form to fill out is finished we will want to send it in a message.
Send **getDataFormToSend()** to the form and add the answer as an extension to
the message to send.

**Examples**

In this example we can see how to create and send a form to fill out:

```
// Create a new form to gather data
Form formToSend = new Form(DataForm.type.form);
formToSend.setInstructions(Fill out this form to report your case.\nThe case will be created automatically.");
formToSend.setTitle("Case configurations");
// Add a hidden variable to the form
FormField field = new FormField("hidden_var");
field.setType(FormField.type.hidden);
field.addValue("Some value for the hidden variable");
formToSend.addField(field);
// Add a fixed variable to the form
field = new FormField();
field.addValue("Section 1: Case description");
formToSend.addField(field);
// Add a text-single variable to the form
field = new FormField("name");
field.setLabel("Enter a name for the case");
field.setType(FormField.type.text_single);
formToSend.addField(field);
// Add a text-multi variable to the form
field = new FormField("description");
field.setLabel("Enter a description");
field.setType(FormField.type.text_multi);
formToSend.addField(field);
// Create a chat with "user2@host.com"
Chat chat = conn1.createChat("user2@host.com" );
Message msg = chat.createMessage();
msg.setBody("To enter a case please fill out this form and send it back");
// Add the form to fill out to the message to send
msg.addExtension(formToSend.getDataFormToSend());
// Send the message with the form to fill out
chat.sendMessage(msg);
```

Answer a Form
-------------

**Description**

Under many situations an XMPP entity could receive a form to fill out. For
example, some hosts may require to fill out a form in order to register new
users. Smack lets the data-providing entity to complete the form in an easy
way and send it back to the data-gathering entity.

**Usage**

The form to fill out contains useful information that could be used for
rendering the form. But it cannot be used to actually complete it. Instead
it's necessary to create a new form based on the original form whose purpose
is to hold all the answers.

In order to create a new _**Form**_ to complete based on the original
_**Form**_ just send **createAnswerForm()** to the original _**Form**_. Once
you have a valid form that could be actually completed all you have to do is
send **setAnswer(String variable, String value)** to the form where variable
is the variable of the _**FormField**_ that you want to answer and value is
the String representation of the answer. If the answer consist of several
values you could then use **setAnswer(String variable, List values)** where
values is a List of Strings.

Once the form has been completed we will want to send it back in a message.
Send **getDataFormToSend()** to the form and add the answer as an extension to
the message to send back.

**Examples**

In this example we can see how to retrieve a form to fill out, complete the
form and send it back:

```
// Get the message with the form to fill out
Message msg2 = chat2.nextMessage();
// Retrieve the form to fill out from the message
Form formToRespond = Form.getFormFrom(msg2);
// Obtain the form to send with the replies
Form completedForm = formToRespond.createAnswerForm();
// Add the answers to the form
completedForm.setAnswer("name", "Credit card number invalid");
completedForm.setAnswer("description", "The ATM says that my credit card number is invalid");
msg2 = chat2.createMessage();
msg2.setBody("To enter a case please fill out this form and send it back"):
// Add the completed form to the message to send back
msg2.addExtension(completedForm.getDataFormToSend());
// Send the message with the completed form
chat2.sendMessage(msg2);
```
