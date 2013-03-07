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
import org.jmxtrans.embedded.util.net.SocketOutputStream;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Factory for {@linkplain org.jmxtrans.embedded.util.net.SocketOutputStream} instances created from {@linkplain java.net.InetSocketAddress}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class SocketOutputStreamPoolFactory extends BaseKeyedPoolableObjectFactory<InetSocketAddress, SocketOutputStream> implements KeyedPoolableObjectFactory<InetSocketAddress, SocketOutputStream> {

    @Override
    public SocketOutputStream makeObject(InetSocketAddress inetSocketAddress) throws Exception {
        Socket socket = new Socket(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        socket.setKeepAlive(true);

        return new SocketOutputStream(socket);
    }

    @Override
    public void destroyObject(InetSocketAddress inetSocketAddress, SocketOutputStream socketOutputStream) throws Exception {
        super.destroyObject(inetSocketAddress, socketOutputStream);
        socketOutputStream.close();
        socketOutputStream.getSocket().close();
    }

    /**
     * Defensive approach: we test all the "<code>Socket.isXXX()</code>" flags.
     */
    @Override
    public boolean validateObject(InetSocketAddress inetSocketAddress, SocketOutputStream socketOutputStream) {
        Socket socket = socketOutputStream.getSocket();
        return socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }
}
