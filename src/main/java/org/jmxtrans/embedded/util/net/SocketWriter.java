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
package org.jmxtrans.embedded.util.net;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Convenience class for writing characters to a {@linkplain Socket}.
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
