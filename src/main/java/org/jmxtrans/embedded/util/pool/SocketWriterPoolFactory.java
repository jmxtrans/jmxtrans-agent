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
import org.jmxtrans.embedded.util.net.SocketWriter;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Factory for {@linkplain SocketWriter} instances created from {@linkplain InetSocketAddress}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class SocketWriterPoolFactory extends BaseKeyedPoolableObjectFactory<InetSocketAddress, SocketWriter> implements KeyedPoolableObjectFactory<InetSocketAddress, SocketWriter> {

    private final Charset charset;

    public SocketWriterPoolFactory(String charset) {
        this(Charset.forName(charset));
    }

    public SocketWriterPoolFactory(Charset charset) {
        this.charset = charset;
    }

    @Override
    public SocketWriter makeObject(InetSocketAddress inetSocketAddress) throws Exception {
        Socket socket = new Socket(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        socket.setKeepAlive(true);

        return new SocketWriter(socket, charset);
    }

    @Override
    public void destroyObject(InetSocketAddress inetSocketAddress, SocketWriter socketWriter) throws Exception {
        super.destroyObject(inetSocketAddress, socketWriter);
        socketWriter.close();
        socketWriter.getSocket().close();
    }

    /**
     * Defensive approach: we test all the "<code>Socket.isXXX()</code>" flags.
     */
    @Override
    public boolean validateObject(InetSocketAddress inetSocketAddress, SocketWriter socketWriter) {
        Socket socket = socketWriter.getSocket();
        return socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }
}
