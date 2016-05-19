/*
 * Copyright (c) 2010-2016 the original author or authors
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
package org.jmxtrans.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.rules.ExternalResource;

/**
 * A server that listens on a TCP port and remembers received plain text lines.
 * Suitable for testing output writers that write plain text lines such as {@link GraphitePlainTextTcpOutputWriter}.
 * Can be used as a JUnit rule for automatic start/stop:
 * 
 * <pre>{@code @Rule
 * public TcpLineServer server = new TcpLineServer();
 * }</pre>
 * 
 * @author Kristoffer Erlandsson
 */
public class TcpLineServer extends ExternalResource {

	private final ServerSocket ss;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<SocketReader> socketReaders = new CopyOnWriteArrayList<>();
	private final List<String> receivedLines = new CopyOnWriteArrayList<>();

	/**
	 * Starts the server on the supplied port.
	 */
	public TcpLineServer(int port) {
		try {
			ss = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Starts the server on a random free port.
	 */
	public TcpLineServer() {
		this(0);
	}

	/**
	 * Returns the port the server is listening on.
	 */
	public int getPort() {
		return ss.getLocalPort();
	}

	/**
	 * Returns all lines that this server has received.
	 */
	public List<String> getReceivedLines() {
		return receivedLines;
	}

	/**
	 * Disconnect all clients that are connected.
	 */
	public void disconnectAllClients() {
		for (SocketReader socketReader : socketReaders) {
			socketReader.disconnect();
		}
		socketReaders.clear();
	}
	
	/**
	 * Starts the server, i.e., starts listening.
	 */
	public void start() {
		executor.execute(acceptor);
	}
	
	/**
	 * Disconnects all clients and stops listening.
	 */
	public void stop() {
		for (SocketReader reader : socketReaders) {
			reader.disconnect();
		}
		try {
			ss.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		executor.shutdownNow();
	}

	@Override
	protected void before() throws Throwable {
		start();
	}

	@Override
	protected void after() {
		stop();
	}


	private Runnable acceptor = new Runnable() {
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Socket accept = ss.accept();
					SocketReader reader = new SocketReader(accept);
					socketReaders.add(reader);
					executor.execute(reader);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	};

	private class SocketReader implements Runnable {

		private final Socket socket;
		private final InputStream is;

		public SocketReader(Socket socket) {
			this.socket = socket;
			try {
				is = socket.getInputStream();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void disconnect() {
			try {
				is.close();
				socket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void run() {
			try {
				readLoop();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void readLoop() throws IOException {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				receivedLines.add(line);
			}
		}

	}

}
