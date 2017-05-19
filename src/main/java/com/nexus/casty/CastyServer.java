package com.nexus.casty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.rmi.CORBA.Util;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CastyServer {

	private static final CastyServer ourInstance = new CastyServer();
	private HttpServer server = null;
	private boolean running = false;


	static CastyServer getInstance() {
		return ourInstance;
	}

	private CastyServer() {}

	void startServer(String hostname, int port) throws IOException, IllegalStateException {
		if (server != null) throw new IllegalStateException("Server already started");
		server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
		server.createContext("/", new CastyHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		running = true;
	}

	void stopServer() {
		if (server != null && isRunning()) server.stop(5);
		server = null;
		running = false;
	}

	boolean isRunning() {
		return running;
	}

	String getAddress() {
		if (server != null) {
			return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
		}
		return null;
	}

	class CastyHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			String method = httpExchange.getRequestMethod();
			switch (method) {
				case "GET":
					handleGet(httpExchange);
					break;
				case "POST":
					handlePost(httpExchange);
					break;
				default:
					httpExchange.close();
					break;
			}
		}

		private void handlePost(HttpExchange httpExchange) throws IOException {
			URI uri = httpExchange.getRequestURI();
			String responseMsg = "OK";
			int responseCode = 200;

			// Get POST data
			InputStream inputStream = httpExchange.getRequestBody();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[2048];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, length);
			}
			String data = outputStream.toString("UTF-8");
			Map<String, String> query = Utils.parseQuery(data);

			switch (uri.getPath()) {
				case "/player/control":
					boolean valid = true, success = true;

					String action = query.get("action");
					if (action == null) {
						responseMsg = "Missing action";
						valid = false;
					}
					else {
						switch (query.get("action")) {
							case "playurl":
								success = CastyPlayer.getInstance().playUrl(URLDecoder.decode(query.get("url"), "UTF-8"));
								break;
							case "playpause":
								success = CastyPlayer.getInstance().playPause();
								break;
							case "position":
								success = CastyPlayer.getInstance().setPosition(Float.valueOf(query.get("percent")));
								break;
							default:
								responseMsg = "Invalid action";
								valid = false;
						}
					}

					if (!valid) {
						responseCode = 400;
					}
					else if (!success) {
						responseMsg = "FAIL";
						responseCode = 500;
					}

					break;
				default:
					responseMsg = "Not Found";
					responseCode = 404;
			}

			byte[] response = responseMsg.getBytes("UTF-8");
			httpExchange.sendResponseHeaders(responseCode, response.length);
			httpExchange.getResponseBody().write(response);
			httpExchange.close();
		}

		private void handleGet(HttpExchange httpExchange) throws IOException {
			String response = null;
			int returnCode = 200;

			Gson gson = new Gson();

			String resourcePath = null;
			String httpPath = httpExchange.getRequestURI().getPath();
			switch (httpPath) {
				case "/":
					resourcePath = "/html/index.html";
					break;
				case "/player/status":
					response = gson.toJson(CastyPlayer.getInstance().getStatus());
					break;
				case "/player/info":
					response = gson.toJson(CastyPlayer.getInstance().getCurrentSongInfo());
					break;
				case "/player/results":
					response = gson.toJson(performSearch(httpExchange.getRequestURI().getQuery()));
				default:
					resourcePath = "/html" + httpPath;
			}

			if (response == null) {
				try {
					// Find file
					URL fileURL = getClass().getResource(resourcePath);
					InputStream stream = getClass().getResourceAsStream(resourcePath);
//					URL fileURL = new File("src/main/resources" + resourcePath).toURI().toURL();
					if (fileURL == null) {
						fileURL = getClass().getResource("/html/404.html");
						returnCode = 404;
					}
					String filePath = fileURL.getPath();

					// Set content type
					String contentType = "";
					switch (filePath.substring(filePath.lastIndexOf('.'))) {
						case ".html":
							contentType = "text/html";
							break;
						case ".css":
							contentType = "text/css";
							break;
						case ".js":
							contentType = "text/javascript";
							break;
						case ".ico":
							contentType = "image/x-icon";
							break;
					}
					if (!contentType.isEmpty()) httpExchange.getResponseHeaders().add("Content-type", contentType);

					httpExchange.sendResponseHeaders(returnCode, 0);

					// Send file
					OutputStream output = httpExchange.getResponseBody();
					int len, BUF_LEN = 1024 * 1024;
					byte[] buf = new byte[BUF_LEN];
					while ((len = stream.read(buf, 0, BUF_LEN)) != -1) output.write(buf, 0, len);


				} catch (NullPointerException | IOException | SecurityException e) {
					System.err.println("Server error: " + httpExchange.getRequestURI().toString());
					e.printStackTrace();
					httpExchange.sendResponseHeaders(500, -1);
				}
			}
			else {
				byte[] data = response.getBytes("UTF-8");
				httpExchange.sendResponseHeaders(returnCode, data.length);
				httpExchange.getResponseBody().write(data);
			}

			httpExchange.close();
		}
	}
	private Map<String, String> performSearch(String query) throws IOException {
		Map<String, String> map = Utils.parseQuery(query);
		if (map.get("q") == null) return null;
		String resultsPage = Utils.downloadURLToString("https://www.youtube.com/results?search_query=" + URLEncoder.encode(map.get("q"),"UTF-8"));

		Map<String, String> videos = new LinkedHashMap<>(20);
		Matcher m = Pattern.compile("\"videoRenderer\"\\s*:\\s*\\{[\\S\\s]*?" +
				"(?:" +
					"(?:" +
						"\"videoId\"\\s*:\\s*\"(?<id1>\\S+)\"[\\S\\s]*?" +
						"\"title\"\\s*:\\s*\\{[\\S\\s]*?\"simpleText\"\\s*:\\s*\"(?<title1>.+?)(?<!\\\\)\")|" +
					"(?:" +
						"\"title\"\\s*:\\s*\\{[\\S\\s]*?\"simpleText\"\\s*:\\s*\"(?<title2>.+?)(?<!\\\\)\"[\\S\\s]*?" +
						"\"videoId\"\\s*:\\s*\"(?<id2>\\S+)\"))")
				.matcher(resultsPage);
		while (m.find()) {
			if (m.group("id1") != null)
				videos.put(m.group("id1"), m.group("title1"));
			if (m.group("id2") != null)
				videos.put(m.group("id2"), m.group("title2"));
		}

		m = Pattern.compile("<a\\s[^>]*?href=\"/watch\\?v=(?<id>[^\\s&\"]+?)\"[^>]*?>(?<title>[^<]*?)</a>")
				.matcher(resultsPage);
		while (m.find()) {
			if (m.group("id") != null && !videos.containsKey(m.group("id")))
				videos.put(m.group("id"), m.group("title"));
		}

		return videos;

	}
}
