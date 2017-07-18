package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.cache.CacheFile;
import com.nexus.casty.player.CastyPlayer;
import com.nexus.casty.Utils;
import com.nexus.casty.song.SongData;
import com.originalnexus.ytd.YTSearch;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class HttpHandler implements com.sun.net.httpserver.HttpHandler {

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
		}
		httpExchange.close();
	}

	private void handlePost(HttpExchange httpExchange) throws IOException {
		URI uri = httpExchange.getRequestURI();
		String responseMsg = "Request OK";
		int responseCode = 200;

		// Get POST queries
		String data = Utils.streamToString(httpExchange.getRequestBody());
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
						case "next":
							success = CastyPlayer.getInstance().next();
							break;
						case "previous":
							success = CastyPlayer.getInstance().previous();
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
					responseMsg = "Request FAILED";
					responseCode = 500;
				}
				break;
			default:
				responseMsg = "Not Found";
				responseCode = 404;
		}

		byte[] response = responseMsg.getBytes(StandardCharsets.UTF_8);
		httpExchange.sendResponseHeaders(responseCode, response.length);
		httpExchange.getResponseBody().write(response);
	}

	private void handleGet(HttpExchange httpExchange) throws IOException {
		String response;
		int returnCode = 200;

		Gson gson = new Gson();

		Map<String, String> queries = Utils.parseQuery(httpExchange.getRequestURI().getQuery());
		String httpPath = httpExchange.getRequestURI().getPath();

		if (httpPath.startsWith("/download/")) {
			handleDownload(httpExchange);
			return;
		}
		switch (httpPath) {
			case "/player/status":
				response = gson.toJson(CastyPlayer.getInstance().getStatus());
				break;
			case "/song/info":
				response = gson.toJson(CastyPlayer.getInstance().getCurrentSongInfo());
				break;
			case "/song/length":
				response = String.valueOf(CastyPlayer.getInstance().getCurrentSongLength());
				break;
			case "/results":
				String query = (queries.get("q") == null) ? "" : queries.get("q");
				response = gson.toJson(YTSearch.performSearch(query));
				break;
			default:
				handleFile(httpExchange);
				return;
		}

		byte[] data = response.getBytes(StandardCharsets.UTF_8);
		httpExchange.sendResponseHeaders(returnCode, data.length);
		httpExchange.getResponseBody().write(data);
	}

	private void handleFile(HttpExchange httpExchange) throws IOException {
		InputStream stream = null;
		String resourcePath;
		int returnCode = 200;

		// Get resource path from url
		resourcePath = httpExchange.getRequestURI().getPath();
		if (resourcePath.equals("/")) resourcePath = "/index.html";
		resourcePath = "/html" + resourcePath;

		try {
			// Check for local files (useful for live updates when developing)
			boolean isLocal = new File("src/main/resources").exists();

			if (isLocal) {
				File f = new File("src/main/resources" + resourcePath);
				stream = f.exists() ? new FileInputStream(f) : null;
			}
			else {
				stream = getClass().getResourceAsStream(resourcePath);
			}

			if (stream == null) {
				resourcePath = "/html/404.html"; returnCode = 404;
				stream = isLocal ? new FileInputStream(new File("src/main/resources" + resourcePath)) : getClass().getResourceAsStream(resourcePath);
				if (stream == null) {
					// Just send the 404 code
					httpExchange.sendResponseHeaders(returnCode, -1);
					return;
				}
			}

			// Set content type to avoid browser warnings
			if (resourcePath.contains(".")) {
				String contentType = "";
				switch (resourcePath.substring(resourcePath.lastIndexOf('.'))) {
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
					case ".jpg":
					case ".jpeg":
						contentType = "image/jpeg";
						break;
					case ".png":
						contentType = "image/png";
						break;
				}
				if (!contentType.isEmpty()) httpExchange.getResponseHeaders().add("Content-Type", contentType);
			}

			// Send file
			httpExchange.sendResponseHeaders(returnCode, 0);
			Utils.streamCopy(stream, httpExchange.getResponseBody());

		} catch (SecurityException e) {
			System.err.println("Server error: " + httpExchange.getRequestURI().toString());
			e.printStackTrace();
		} finally {
			try { if (stream != null) stream.close(); } catch (IOException ignored) {}
		}
	}

	private void handleDownload(HttpExchange httpExchange) throws IOException {
		boolean found = false;

		String path = httpExchange.getRequestURI().getPath().substring("/download/".length());
		int i = path.indexOf('/');
		if (i >= 0) {
			String prefix = path.substring(0, i);
			String id = path.substring(i + 1);

			CacheFile f = CastyPlayer.getInstance().getCache().getCacheFile(prefix, id);
			if (f != null) {
				f.getLocks().read();
				try {
					if (f.exists() && f.isFile()) {
						found = true;
						String header = "attachment";
						SongData data = f.getData();
						if (data != null) {
							if (data.filename != null && !data.filename.isEmpty()) {
								header = "attachment; filename=\"" + data.filename + "\"";
							}
							if (data.container != null && !data.container.isEmpty()) {
								httpExchange.getResponseHeaders().add("Content-Type", "audio/" + data.container);
							}
						}
						httpExchange.getResponseHeaders().add("Content-Disposition", header);

						httpExchange.sendResponseHeaders(200, f.length());
						Utils.fileToStream(f, httpExchange.getResponseBody());
					}
				} finally {
					f.getLocks().readRelease();
				}
			}
		}

		if (!found) httpExchange.sendResponseHeaders(404, -1);
	}

}