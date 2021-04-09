package org.jivesoftware.smack.sasl.gssApi;

import org.ietf.jgss.*;

import java.io.InputStream;
import java.io.OutputStream;

public class GssContext implements GSSContext {
    @Override
    public byte[] initSecContext(byte[] bytes, int i, int i1) throws GSSException {
        return new byte[0];
    }

    @Override
    public int initSecContext(InputStream inputStream, OutputStream outputStream) throws GSSException {
        return 0;
    }

    @Override
    public byte[] acceptSecContext(byte[] bytes, int i, int i1) throws GSSException {
        return new byte[0];
    }

    @Override
    public void acceptSecContext(InputStream inputStream, OutputStream outputStream) throws GSSException {

    }

    @Override
    public boolean isEstablished() {
        return false;
    }

    @Override
    public void dispose() throws GSSException {

    }

    @Override
    public int getWrapSizeLimit(int i, boolean b, int i1) throws GSSException {
        return 0;
    }

    @Override
    public byte[] wrap(byte[] bytes, int i, int i1, MessageProp messageProp) throws GSSException {
        return new byte[0];
    }

    @Override
    public void wrap(InputStream inputStream, OutputStream outputStream, MessageProp messageProp) throws GSSException {

    }

    @Override
    public byte[] unwrap(byte[] bytes, int i, int i1, MessageProp messageProp) throws GSSException {
        return new byte[0];
    }

    @Override
    public void unwrap(InputStream inputStream, OutputStream outputStream, MessageProp messageProp) throws GSSException {

    }

    @Override
    public byte[] getMIC(byte[] bytes, int i, int i1, MessageProp messageProp) throws GSSException {
        return new byte[0];
    }

    @Override
    public void getMIC(InputStream inputStream, OutputStream outputStream, MessageProp messageProp) throws GSSException {

    }

    @Override
    public void verifyMIC(byte[] bytes, int i, int i1, byte[] bytes1, int i2, int i3, MessageProp messageProp) throws GSSException {

    }

    @Override
    public void verifyMIC(InputStream inputStream, InputStream inputStream1, MessageProp messageProp) throws GSSException {

    }

    @Override
    public byte[] export() throws GSSException {
        return new byte[0];
    }

    @Override
    public void requestMutualAuth(boolean b) throws GSSException {

    }

    @Override
    public void requestReplayDet(boolean b) throws GSSException {

    }

    @Override
    public void requestSequenceDet(boolean b) throws GSSException {

    }

    @Override
    public void requestCredDeleg(boolean b) throws GSSException {

    }

    @Override
    public void requestAnonymity(boolean b) throws GSSException {

    }

    @Override
    public void requestConf(boolean b) throws GSSException {

    }

    @Override
    public void requestInteg(boolean b) throws GSSException {

    }

    @Override
    public void requestLifetime(int i) throws GSSException {

    }

    @Override
    public void setChannelBinding(ChannelBinding channelBinding) throws GSSException {

    }

    @Override
    public boolean getCredDelegState() {
        return false;
    }

    @Override
    public boolean getMutualAuthState() {
        return false;
    }

    @Override
    public boolean getReplayDetState() {
        return false;
    }

    @Override
    public boolean getSequenceDetState() {
        return false;
    }

    @Override
    public boolean getAnonymityState() {
        return false;
    }

    @Override
    public boolean isTransferable() throws GSSException {
        return false;
    }

    @Override
    public boolean isProtReady() {
        return false;
    }

    @Override
    public boolean getConfState() {
        return false;
    }

    @Override
    public boolean getIntegState() {
        return false;
    }

    @Override
    public int getLifetime() {
        return 0;
    }

    @Override
    public GSSName getSrcName() throws GSSException {
        return null;
    }

    @Override
    public GSSName getTargName() throws GSSException {
        return null;
    }

    @Override
    public Oid getMech() throws GSSException {
        return null;
    }

    @Override
    public GSSCredential getDelegCred() throws GSSException {
        return null;
    }

    @Override
    public boolean isInitiator() throws GSSException {
        return false;
    }
}
