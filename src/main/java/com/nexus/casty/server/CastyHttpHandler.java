package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.cache.CacheFile;
import com.nexus.casty.player.CastyPlayer;
import com.nexus.casty.Utils;
import com.nexus.casty.player.PlaylistItem;
import com.nexus.casty.song.SongData;
import com.originalnexus.ytd.YTSearch;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

class CastyHttpHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setCharacterEncoding("UTF-8");

		switch (request.getMethod()) {
			case "GET":
				if (handleGet(request, response))
					baseRequest.setHandled(true);
				break;
			case "POST":
				handlePost(request, response);
				baseRequest.setHandled(true);
				break;
		}
	}

	private void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean success = true;
		switch (request.getRequestURI()) {
			case "/player/control":
				String action = request.getParameter("action");
				if (action == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action");
					return;
				}
				switch (request.getParameter("action")) {
					case "playurl":
						success = CastyPlayer.getInstance().playUrl(URLDecoder.decode(request.getParameter("url"), StandardCharsets.UTF_8));
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
						success = CastyPlayer.getInstance().setPosition(Float.valueOf(request.getParameter("percent")));
						break;
					case "playlist":
						int index = -1;
						if (request.getParameter("index") != null) {
							try {
								index = Integer.valueOf(request.getParameter("index"));
							} catch (NumberFormatException ignored) {}
						}
						if (index < 0) {
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing index");
							return;
						}
						success = CastyPlayer.getInstance().playlistPlayItem(index);

						break;
					case "volume":
						int volume = -1;
						String level = request.getParameter("level");
						if (level != null) {
							try {
								volume = Integer.valueOf(level);
							} catch (NumberFormatException ignored) {}
						}
						if (volume < 0) {
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing volume level");
							return;
						}
						success = CastyPlayer.getInstance().setVolume(volume);
						break;
					default:
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
						return;
				}

				break;
			case "/playlist/add":
				String url = request.getParameter("url");
				if (url == null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing url");
					return;
				}
				String title = request.getParameter("title");
				if (title == null) title = url;

				PlaylistItem item = new PlaylistItem();
				item.url = URLDecoder.decode(url, StandardCharsets.UTF_8);
				item.title = URLDecoder.decode(title, StandardCharsets.UTF_8);
				success = CastyPlayer.getInstance().getPlaylist().addItem(item);
				break;
			case "/playlist/remove":
				int index = -1;
				if (request.getParameter("index") != null) {
					try {
						index = Integer.valueOf(request.getParameter("index"));
					} catch (NumberFormatException ignored) {}
				}

				if (index < 0) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing index");
					return;
				}
				success = CastyPlayer.getInstance().getPlaylist().removeItem(index);
				break;

			case "/playlist/control":
				String repeat = request.getParameter("repeat");
				if (repeat != null) {
					switch (repeat.toLowerCase()) {
						case "true":
							CastyPlayer.getInstance().getPlaylist().setRepeat(true);
							break;
						case "false":
							CastyPlayer.getInstance().getPlaylist().setRepeat(false);
							break;
						default:
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing repeat");
							return;
					}
				}
				break;
			case "/playlist/cache":
				success = CastyPlayer.getInstance().loadCachePlaylist();
				break;
			default:
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
		}

		if (!success)
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

	}

	private boolean handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String responseBody;

		Gson gson = new Gson();

		String httpPath = request.getRequestURI();

		if (httpPath.startsWith("/download/")) {
			handleDownload(request, response);
			return true;
		}
		switch (httpPath) {
			case "/player/status":
				responseBody = gson.toJson(CastyPlayer.getInstance().getStatus());
				break;
			case "/song/info":
				responseBody = gson.toJson(CastyPlayer.getInstance().getCurrentSongInfo());
				break;
			case "/song/length":
				responseBody = String.valueOf(CastyPlayer.getInstance().getCurrentSongLength());
				break;
			case "/results":
				String query = (request.getParameter("q") == null) ? "" : request.getParameter("q");
				responseBody = gson.toJson(YTSearch.performSearch(query));
				break;
			case "/playlist/list":
				responseBody = gson.toJson(CastyPlayer.getInstance().getPlaylist().toArray());
				break;
			default:
				return false;
		}

		response.getOutputStream().print(responseBody);
		return true;
	}

	private void handleDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean found = false;

		String path = request.getRequestURI().substring("/download/".length());
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
								response.setHeader("Content-Type", "audio/" + data.container);
							}
						}
						response.setHeader("Content-Disposition", header);

						Utils.fileToStream(f, response.getOutputStream());
					}
				} finally {
					f.getLocks().readRelease();
				}
			}
		}

		if (!found) response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

}
