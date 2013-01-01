/*
 * Copyright 1012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
