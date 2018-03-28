File Transfer
=============

[Back](index.md)

The file transfer extension allows the user to transmit and receive files.

  * Send a file to another user
  * Recieving a file from another user
  * Monitoring the progress of a file transfer

**XEP related:** [XEP-95](http://www.xmpp.org/extensions/xep-0095.html) [XEP-96](http://www.xmpp.org/extensions/xep-0096.html) [XEP-65](http://www.xmpp.org/extensions/xep-0065.html) [XEP-47](http://www.xmpp.org/extensions/xep-0047.html)

Send a file to another user
---------------------------

**Description**

A user may wish to send a file to another user. The other user has the option
of acception, rejecting, or ignoring the users request. Smack provides a
simple interface in order to enable the user to easily send a file. **Usage**

In order to send a file you must first construct an instance of the
**_FileTransferManager_** class. In order to instantiate the manager
you should call _FileTransferManager.getInstanceFor(connection)_

Once you have your **_FileTransferManager_** you will need to create an
outgoing file transfer to send a file. The method to use on the
**_FileTransferManager_** is the **createOutgoingFileTransfer(userID)**
method. The userID you provide to this method is the fully-qualified jabber ID
of the user you wish to send the file to. A fully-qualified jabber ID consists
of a node, a domain, and a resource, the user must be connected to the
resource in order to be able to recieve the file transfer.

Now that you have your **_OutgoingFileTransfer_** instance you will want to
send the file. The method to send a file is **sendFile(file, description)**.
The file you provide to this method should be a readable file on the local
file system, and the description is a short description of the file to help
the user decide whether or not they would like to recieve the file.

For information on monitoring the progress of a file transfer see the
monitoring progress section of this document.

Other means to send a file are also provided as part of the
**_OutgoingFileTransfer_**. Please consult the Javadoc for more information.
**Examples**

In this example we can see how to send a file:

```
// Create the file transfer manager
FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
// Create the outgoing file transfer
OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer("romeo@montague.net");
// Send the file
transfer.sendFile(new File("shakespeare_complete_works.txt"), "You won't believe this!");
```

Recieving a file from another user
----------------------------------

**Description**

The user may wish to recieve files from another user. The process of recieving
a file is event driven, new file transfer requests are recieved from other
users via a listener registered with the file transfer manager.

**Usage**

In order to recieve a file you must first construct an instance of the
**_FileTransferManager_** class. This class has one constructor with one
parameter which is your XMPPConnection. In order to instantiate the manager
you should call _FileTransferManager.getInstanceFor(connection)_

Once you have your **_FileTransferManager_** you will need to register a
listener with it. The FileTransferListner interface has one method,
**fileTransferRequest(request)**. When a request is recieved through this
method, you can either accept or reject the request. To help you make your
decision there are several methods in the **_FileTransferRequest_** class that
return information about the transfer request.

To accept the file transfer, call the **accept()**, this method will create an
**_IncomingFileTransfer_**. After you have the file transfer you may start to
transfer the file by calling the **recieveFile(file)** method. The file
provided to this method will be where the data from thefile transfer is saved.

Finally, to reject the file transfer the only method you need to call is
**reject()** on the **_IncomingFileTransfer_**.

For information on monitoring the progress of a file transfer see the
monitoring progress section of this document.

Other means to recieve a file are also provided as part of the
**_IncomingFileTransfer_**. Please consult the Javadoc for more information.
**Examples**

In this example we can see how to approve or reject a file transfer request:

```
// Create the file transfer manager
final FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
// Create the listener
manager.addFileTransferListener(new FileTransferListener() {
	public void fileTransferRequest(FileTransferRequest request) {
	// Check to see if the request should be accepted
	if(shouldAccept(request)) {
		// Accept it
		IncomingFileTransfer transfer = request.accept();
		transfer.recieveFile(new File("shakespeare_complete_works.txt"));
	} else {
		// Reject it
		request.reject();
	}
}
});
```

Monitoring the progress of a file transfer
------------------------------------------

**Description**

While a file transfer is in progress you may wish to monitor the progress of a
file transfer.

**Usage**

Both the **_IncomingFileTransfer_** and the **_OutgoingFileTransfer_** extend
the **_FileTransfer_** class which provides several methods to monitor how a
file transfer is progressing:

  * **getStatus()** - The file transfer can be in several states, negotiating, rejected, canceled, in progress, error, and complete. This method will return which state the file transfer is currently in. 
  * **getProgress()** - if the status of the file transfer is in progress this method will return a number between 0 and 1, 0 being the transfer has not yet started and 1 being the transfer is complete. It may also return a -1 if the transfer is not in progress. 
  * **isDone()** - Similar to getProgress() except it returns a _boolean_. If the state is rejected, canceled, error, or complete then true will be returned and false otherwise. 
  * **getError()** - If there is an error during the file transfer this method will return the type of error that occured.  **Examples**

In this example we can see how to monitor a file transfer:

```
while(!transfer.isDone()) {
	if(transfer.getStatus().equals(Status.ERROR)) {
		System.out.println("ERROR!!! " + transfer.getError());
	} else {
		System.out.println(transfer.getStatus());
		System.out.println(transfer.getProgress());
	}
	sleep(1000);
}
```
