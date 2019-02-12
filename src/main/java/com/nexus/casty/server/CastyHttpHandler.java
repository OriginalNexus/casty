package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.player.CastyPlayer;
import com.nexus.casty.song.SongData;
import com.nexus.ytd.YTSearch;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

class CastyHttpHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (request.getMethod().equals("GET") && handleGet(request, response))
			baseRequest.setHandled(true);
	}

	private boolean handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String responseBody;
		Gson gson = new Gson();
		String httpPath = request.getRequestURI();

		response.setCharacterEncoding("UTF-8");

		if (httpPath.startsWith("/download/")) {
			handleDownload(request, response);
			return true;
		}
		if ("/results".equals(httpPath)) {
			String query = request.getParameter("q");
			if (query == null)
				query = "";
			responseBody = gson.toJson(YTSearch.performSearch(query));
		} else {
			return false;
		}

		response.getOutputStream().print(responseBody);
		return true;
	}

	private void handleDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {
		boolean found = false;

		String id = request.getRequestURI().substring("/download/".length());

		SongData data = CastyPlayer.getInstance().getCache().getData(id);
		if (data != null) {
			File f = CastyPlayer.getInstance().getCache().getFile(id);

			if (f.exists() && f.isFile()) {
				found = true;
				String header = "attachment";

				if (data.filename != null && !data.filename.isEmpty()) {
					header = "attachment; filename=\"" + data.filename + "\"";
				}
				if (data.container != null && !data.container.isEmpty()) {
					response.setHeader("Content-Type", "audio/" + data.container);
				}
				response.setHeader("Content-Disposition", header);

				try (FileInputStream fs = new FileInputStream(f)) {
					fs.transferTo(response.getOutputStream());
				}
			}
		}

		if (!found)
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

}
