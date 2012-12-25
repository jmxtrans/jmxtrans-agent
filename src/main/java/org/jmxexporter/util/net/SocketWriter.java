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
package org.jmxexporter.util.net;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Convenience class for writing character to a {@linkplain Socket}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class SocketWriter extends FilterWriter {

    private final Socket socket;

    /**
     * @param inetSocketAddress host and port of the underlying {@linkplain Socket}
     * @param charset           charset of the {@linkplain java.io.OutputStream} underlying  {@linkplain Socket}
     * @throws IOException
     */
    public SocketWriter(InetSocketAddress inetSocketAddress, Charset charset) throws IOException {
        this(new Socket(inetSocketAddress.getAddress(), inetSocketAddress.getPort()), charset);
    }

    public SocketWriter(Socket socket, Charset charset) throws IOException {
        super(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset)));
        this.socket = socket;
    }

    /**
     * Return the underlying {@linkplain java.net.Socket}
     */
    public Socket getSocket() {
        return socket;
    }

    @Override
    public String toString() {
        return "SocketWriter{" +
                "socket=" + socket +
                '}';
    }
}
