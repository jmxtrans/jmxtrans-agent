/*
 * Copyright 2008-2012 Xebia and the original author or authors.
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
package org.jmxexporter.util.pool;

import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class SocketFactory extends BaseKeyedPoolableObjectFactory<InetSocketAddress, Socket> implements KeyedPoolableObjectFactory<InetSocketAddress, Socket> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Socket makeObject(InetSocketAddress inetSocketAddress) throws Exception {
        Socket socket = new Socket(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        socket.setKeepAlive(true);
        return socket;
    }

    @Override
    public void destroyObject(InetSocketAddress inetSocketAddress, Socket socket) throws Exception {
        super.destroyObject(inetSocketAddress, socket);
        socket.close();
    }

    @Override
    public boolean validateObject(InetSocketAddress inetSocketAddress, Socket socket) {
        return socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }

    @Override
    public int hashCode() {
        return getClass().getSimpleName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass().equals(SocketFactory.class);
    }
}
