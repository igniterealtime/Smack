#!/usr/bin/env bash

set -e

# Pretty fancy method to get reliable the absolute path of a shell
# script, *even if it is sourced*. Credits go to GreenFox on
# stackoverflow: http://stackoverflow.com/a/12197518/194894
pushd . > /dev/null
SCRIPTDIR="${BASH_SOURCE[0]}";
while([ -h "${SCRIPTDIR}" ]); do
    cd "`dirname "${SCRIPTDIR}"`"
    SCRIPTDIR="$(readlink "`basename "${SCRIPTDIR}"`")";
done
cd "`dirname "${SCRIPTDIR}"`" > /dev/null
SCRIPTDIR="`pwd`";
popd  > /dev/null

SMACK_DIR=$(readlink "${SCRIPTDIR}"/..)

FIND_ALL_JAVA_SRC="find ${SMACK_DIR} \
	 -type f \
	 -name *.java \
	 -print"

declare -A SMACK_EXCEPTIONS
SMACK_EXCEPTIONS[NotConnectedException]="if the XMPP connection is not connected."
SMACK_EXCEPTIONS[InterruptedException]="if the calling thread was interrupted."
SMACK_EXCEPTIONS[XMPPErrorException]="if there was an XMPP error returned."
SMACK_EXCEPTIONS[NoResponseException]="if there was no response from the remote entity."
SMACK_EXCEPTIONS[NotLoggedInException]="if the XMPP connection is not authenticated."
SMACK_EXCEPTIONS[BOSHException]="if an BOSH related error occured."
SMACK_EXCEPTIONS[IOException]="if an I/O error occured."
SMACK_EXCEPTIONS[SmackException]="if Smack detected an exceptional situation."
SMACK_EXCEPTIONS[XMPPException]="if an XMPP protocol error was received."
SMACK_EXCEPTIONS[SmackSaslException]="if a SASL specific error occured."
SMACK_EXCEPTIONS[SASLErrorException]="if a SASL protocol error was returned."
SMACK_EXCEPTIONS[NotAMucServiceException]="if the entity is not a MUC serivce."
SMACK_EXCEPTIONS[NoSuchAlgorithmException]="if no such algorithm is available."
SMACK_EXCEPTIONS[KeyManagementException]="if there was a key mangement error."
SMACK_EXCEPTIONS[XmppStringprepException]="if the provided string is invalid."
SMACK_EXCEPTIONS[XmlPullParserException]="if an error in the XML parser occured."
SMACK_EXCEPTIONS[SmackParsingException]="if the Smack parser (provider) encountered invalid input."
SMACK_EXCEPTIONS[MucNotJoinedException]="if not joined to the Multi-User Chat."
SMACK_EXCEPTIONS[MucAlreadyJoinedException]="if already joined the Multi-User Chat."7y
SMACK_EXCEPTIONS[NotALeafNodeException]="if a PubSub leaf node operation was attempted on a non-leaf node."
SMACK_EXCEPTIONS[FeatureNotSupportedException]="if a requested feature is not supported by the remote entity."
SMACK_EXCEPTIONS[MucConfigurationNotSupportedException]="if the requested MUC configuration is not supported by the MUC service."
SMACK_EXCEPTIONS[CouldNotConnectToAnyProvidedSocks5Host]="if no connection to any provided stream host could be established"
SMACK_EXCEPTIONS[NoSocks5StreamHostsProvided]="if no stream host was provided."
SMACK_EXCEPTIONS[SmackMessageException]="if there was an error."
SMACK_EXCEPTIONS[SecurityException]="if there was a security violation."
SMACK_EXCEPTIONS[InvocationTargetException]="if a reflection-based method or constructor invocation threw."
SMACK_EXCEPTIONS[IllegalArgumentException]="if an illegal argument was given."
SMACK_EXCEPTIONS[NotAPubSubNodeException]="if a involved node is not a PubSub node."
SMACK_EXCEPTIONS[NoAcceptableTransferMechanisms]="if no acceptable transfer mechanisms are available"
SMACK_EXCEPTIONS[NoSuchMethodException]="if no such method is declared"
SMACK_EXCEPTIONS[Exception]="if an exception occured."
SMACK_EXCEPTIONS[TestNotPossibleException]="if the test is not possible."
SMACK_EXCEPTIONS[TimeoutException]="if there was a timeout."
SMACK_EXCEPTIONS[IllegalStateException]="if an illegal state was encountered"
SMACK_EXCEPTIONS[NoSuchPaddingException]="if the requested padding mechanism is not availble."
SMACK_EXCEPTIONS[BadPaddingException]="if the input data is not padded properly."
SMACK_EXCEPTIONS[InvalidKeyException]="if the key is invalid."
SMACK_EXCEPTIONS[IllegalBlockSizeException]="if the input data length is incorrect."
SMACK_EXCEPTIONS[InvalidAlgorithmParameterException]="if the provided arguments are invalid."
SMACK_EXCEPTIONS[CorruptedOmemoKeyException]="if the OMEMO key is corrupted."
SMACK_EXCEPTIONS[CryptoFailedException]="if the OMEMO cryptography failed."
SMACK_EXCEPTIONS[CannotEstablishOmemoSessionException]="if no OMEMO session could be established."
SMACK_EXCEPTIONS[UntrustedOmemoIdentityException]="if the OMEMO identity is not trusted."

MODE=""

while getopts dm: OPTION "$@"; do
	case $OPTION in
	d)
		set -x
		;;
	m)
		MODE=${OPTARG}
		;;
	*)
		echo "Unknown option ${OPTION}"
		exit 1
		;;
	esac
done

sed_sources() {
	sedScript=${1}
	${FIND_ALL_JAVA_SRC} |\
		xargs sed \
			  --in-place \
			  --follow-symlinks \
			  --regexp-extended \
			  "${sedScript}"
}

show_affected() {
	echo ${!SMACK_EXCEPTIONS{@}}
	for exception in ${!SMACK_EXCEPTIONS[@]}; do
		${FIND_ALL_JAVA_SRC} |\
			xargs grep " \* @throws $exception$" || true
	done
	for exception in ${!SMACK_EXCEPTIONS[@]}; do
		count=$(${FIND_ALL_JAVA_SRC} |\
					xargs grep " \* @throws $exception$" | wc -l)
		echo "$exception $count"
	done

}

fix_affected() {
	for exception in "${!SMACK_EXCEPTIONS[@]}"; do
		exceptionJavadoc=${SMACK_EXCEPTIONS[${exception}]}
		sed_sources "s;@throws ((\w*\.)?${exception})\$;@throws \1 ${exceptionJavadoc};"
	done
}

add_todo_to_param_and_return() {
	sed_sources "s;@(param|return) (\w*)\$;@\1 \2 TODO javadoc me please;"
}

case $MODE in
	show)
		show_affected
		;;
	fix)
		fix_affected
		;;
	*)
		echo "Unknown mode ${mode}"
		exit 1
esac
