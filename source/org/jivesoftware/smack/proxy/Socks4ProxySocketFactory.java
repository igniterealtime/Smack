package org.jivesoftware.smack.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.SocketFactory;

/**
 * Socket factory for socks4 proxy 
 *  
 * @author Atul Aggarwal
 */
public class Socks4ProxySocketFactory 
    extends SocketFactory
{
    private ProxyInfo proxy;
    
    public Socks4ProxySocketFactory(ProxyInfo proxy)
    {
        this.proxy = proxy;
    }

    public Socket createSocket(String host, int port) 
        throws IOException, UnknownHostException
    {
        return socks4ProxifiedSocket(host,port);
        
    }

    public Socket createSocket(String host ,int port, InetAddress localHost,
                                int localPort)
        throws IOException, UnknownHostException
    {
        return socks4ProxifiedSocket(host,port);
    }

    public Socket createSocket(InetAddress host, int port)
        throws IOException
    {
        return socks4ProxifiedSocket(host.getHostAddress(),port);
    }

    public Socket createSocket( InetAddress address, int port, 
                                InetAddress localAddress, int localPort) 
        throws IOException
    {
        return socks4ProxifiedSocket(address.getHostAddress(),port);
        
    }
    
    private Socket socks4ProxifiedSocket(String host, int port) 
        throws IOException
    {
        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;
        String proxy_host = proxy.getProxyAddress();
        int proxy_port = proxy.getProxyPort();
        String user = proxy.getProxyUsername();
        String passwd = proxy.getProxyPassword();
        
        try
        {
            socket=new Socket(proxy_host, proxy_port);    
            in=socket.getInputStream();
            out=socket.getOutputStream();
            socket.setTcpNoDelay(true);
            
            byte[] buf=new byte[1024];
            int index=0;

    /*
    1) CONNECT

    The client connects to the SOCKS server and sends a CONNECT request when
    it wants to establish a connection to an application server. The client
    includes in the request packet the IP address and the port number of the
    destination host, and userid, in the following format.

           +----+----+----+----+----+----+----+----+----+----+....+----+
           | VN | CD | DSTPORT |      DSTIP        | USERID       |NULL|
           +----+----+----+----+----+----+----+----+----+----+....+----+
    # of bytes:   1    1      2              4           variable       1

    VN is the SOCKS protocol version number and should be 4. CD is the
    SOCKS command code and should be 1 for CONNECT request. NULL is a byte
    of all zero bits.
    */

            index=0;
            buf[index++]=4;
            buf[index++]=1;

            buf[index++]=(byte)(port>>>8);
            buf[index++]=(byte)(port&0xff);

            try
            {
                InetAddress addr=InetAddress.getByName(host);
                byte[] byteAddress = addr.getAddress();
                for (int i = 0; i < byteAddress.length; i++) 
                {
                    buf[index++]=byteAddress[i];
                }
            }
            catch(UnknownHostException uhe)
            {
                throw new ProxyException(ProxyInfo.ProxyType.SOCKS4, 
                    uhe.toString(), uhe);
            }

            if(user!=null)
            {
                System.arraycopy(user.getBytes(), 0, buf, index, user.length());
                index+=user.length();
            }
            buf[index++]=0;
            out.write(buf, 0, index);

    /*
    The SOCKS server checks to see whether such a request should be granted
    based on any combination of source IP address, destination IP address,
    destination port number, the userid, and information it may obtain by
    consulting IDENT, cf. RFC 1413.  If the request is granted, the SOCKS
    server makes a connection to the specified port of the destination host.
    A reply packet is sent to the client when this connection is established,
    or when the request is rejected or the operation fails. 

           +----+----+----+----+----+----+----+----+
           | VN | CD | DSTPORT |      DSTIP        |
           +----+----+----+----+----+----+----+----+
    # of bytes:   1    1      2              4

    VN is the version of the reply code and should be 0. CD is the result
    code with one of the following values:

    90: request granted
    91: request rejected or failed
    92: request rejected becasue SOCKS server cannot connect to
    identd on the client
    93: request rejected because the client program and identd
    report different user-ids

    The remaining fields are ignored.
    */

            int len=6;
            int s=0;
            while(s<len)
            {
                int i=in.read(buf, s, len-s);
                if(i<=0)
                {
                    throw new ProxyException(ProxyInfo.ProxyType.SOCKS4, 
                        "stream is closed");
                }
                s+=i;
            }
            if(buf[0]!=0)
            {
                throw new ProxyException(ProxyInfo.ProxyType.SOCKS4, 
                    "server returns VN "+buf[0]);
            }
            if(buf[1]!=90)
            {
                try
                {
                    socket.close();
                }
                catch(Exception eee)
                {
                }
                String message="ProxySOCKS4: server returns CD "+buf[1];
                throw new ProxyException(ProxyInfo.ProxyType.SOCKS4,message);
            }
            byte[] temp = new byte[2];
            in.read(temp, 0, 2);
            return socket;
        }
        catch(RuntimeException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            try
            {
                if(socket!=null)socket.close(); 
            }
            catch(Exception eee)
            {
            }
            throw new ProxyException(ProxyInfo.ProxyType.SOCKS4, e.toString());
        }
    }
}
