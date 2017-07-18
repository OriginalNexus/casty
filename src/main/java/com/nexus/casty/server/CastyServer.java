package com.nexus.casty.server;

import com.nexus.casty.player.CastyPlayer;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class CastyServer {

	public static final int DEFAULT_PORT = 5555;

	private static final CastyServer ourInstance = new CastyServer();
	private HttpServer server = null;
	private boolean running = false;

	public static CastyServer getInstance() {
		return ourInstance;
	}

	private CastyServer() {}

	public synchronized void startServer(String hostname, int port) throws IOException, IllegalStateException {
		if (server != null) throw new IllegalStateException("CastyServer already started");
		server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
		server.createContext("/", new HttpHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		running = true;
	}

	public synchronized void stopServer() {
		if (server != null && isRunning()) {
			server.stop(5);
			CastyPlayer.getInstance().reset();
		}
		server = null;
		running = false;
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized String getAddress() {
		if (server != null) {
			return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
		}
		return null;
	}

	public static String getHostAddress() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException ignored) {}
		return Inet4Address.getLoopbackAddress().getHostAddress();
	}


}
