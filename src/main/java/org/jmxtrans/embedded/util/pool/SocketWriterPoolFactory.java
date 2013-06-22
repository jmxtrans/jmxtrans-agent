/*
 * Copyright (c) 2010-2013 the original author or authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package org.jmxtrans.embedded.util.pool;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.jmxtrans.embedded.util.net.HostAndPort;
import org.jmxtrans.embedded.util.net.SocketWriter;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Factory for {@linkplain SocketWriter} instances created from {@linkplain HostAndPort}.
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class SocketWriterPoolFactory extends BaseKeyedPoolableObjectFactory<HostAndPort, SocketWriter> implements KeyedPoolableObjectFactory<HostAndPort, SocketWriter> {

    public static final int DEFAULT_SOCKET_CONNECT_TIMEOUT_IN_MILLIS = 500;
    private final Charset charset;
    private final int socketConnectTimeoutInMillis;

    public SocketWriterPoolFactory(String charset, int socketConnectTimeoutInMillis) {
        this(Charset.forName(charset), socketConnectTimeoutInMillis);
    }

    public SocketWriterPoolFactory(Charset charset, int socketConnectTimeoutInMillis) {
        this.charset = charset;
        this.socketConnectTimeoutInMillis = socketConnectTimeoutInMillis;
    }

    @Override
    public SocketWriter makeObject(HostAndPort HostAndPort) throws Exception {
        Socket socket = new Socket();
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(HostAndPort.getHost(), HostAndPort.getPort()), socketConnectTimeoutInMillis);

        return new SocketWriter(socket, charset);
    }

    @Override
    public void destroyObject(HostAndPort HostAndPort, SocketWriter socketWriter) throws Exception {
        super.destroyObject(HostAndPort, socketWriter);
        socketWriter.close();
        socketWriter.getSocket().close();
    }

    /**
     * Defensive approach: we test all the "<code>Socket.isXXX()</code>" flags.
     */
    @Override
    public boolean validateObject(HostAndPort HostAndPort, SocketWriter socketWriter) {
        Socket socket = socketWriter.getSocket();
        return socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }

}
