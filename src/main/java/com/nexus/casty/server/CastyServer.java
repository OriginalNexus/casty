package com.nexus.casty.server;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CastyServer {

	public static final int DEFAULT_PORT = 5555;

	private static final CastyServer ourInstance = new CastyServer();
	private final Server server;
	private final ServerConnector serverConnector;
	private boolean running = false;

	final List<StatusListener> listeners = new ArrayList<>();
	final List<Supplier<Object>> suppliers = new ArrayList<>();

	public static CastyServer getInstance() {
		return ourInstance;
	}

	private CastyServer() {
		server = new Server();
		serverConnector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration() {
			{
				this.setSendServerVersion(false);
			}
		}));
		server.setConnectors(new Connector[] {serverConnector});

		// Static resources handler
		String localDevPath = "src/main/resources/html";
		boolean isDev = new File(localDevPath).exists();
		String resourcesPath = isDev ? localDevPath : getClass().getResource("/html").toExternalForm();

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase(resourcesPath);
		resourceHandler.setDirectoriesListed(false);

		// Error handler
		ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
		errorPageErrorHandler.addErrorPage(HttpServletResponse.SC_NOT_FOUND, "/404.html");
		errorPageErrorHandler.addErrorPage(HttpServletResponse.SC_FORBIDDEN, "/403.html");

		// HTTP context handler (custom + static resources + error handlers)
		ContextHandler httpContextHandler = new ContextHandler("/");
		httpContextHandler.setHandler(new HandlerCollection(new CastyHttpHandler(), resourceHandler, new DefaultHandler()));
		httpContextHandler.setErrorHandler(errorPageErrorHandler);

		// WebSockets handler
		ContextHandler wsContextHandler = new ContextHandler("/ws");
		wsContextHandler.setHandler(new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.getPolicy().setIdleTimeout(0); // No timeout
				factory.register(CastyWebSocketHandler.class);
			}
		});

		server.setHandler(new ContextHandlerCollection(wsContextHandler, httpContextHandler));
	}

	public synchronized void startServer(String hostname, int port) throws Exception {
		if (running)
			throw new IllegalStateException("Server already running!");

		serverConnector.setPort(port);
		serverConnector.setHost(hostname);
		server.start();

		running = true;
	}

	public synchronized void stopServer() throws Exception {
		if (running)
			server.stop();
		running = false;
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public synchronized String getAddress() {
		return "http://" + serverConnector.getHost() + ":" + serverConnector.getPort();
	}

	public static String getHostAddress() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException ignored) {}
		return Inet4Address.getLoopbackAddress().getHostAddress();
	}

	public synchronized void updateStatus(Object status) {
		if (!running)
			throw new IllegalStateException("Server must be running");

		listeners.removeIf(statusListener -> !statusListener.session.isOpen());
		for (StatusListener l : listeners)
			l.onUpdate(status);
	}

	public synchronized void registerStatusSupplier(Supplier<Object> supplier) {
		suppliers.add(supplier);
	}

}
