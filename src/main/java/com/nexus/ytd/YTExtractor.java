package com.nexus.ytd;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTExtractor {

	private static final String[] el_array = new String[] { "", "&el=vevo", "&el=detailpage", "&el=embedded" };
	private static final String EN_US_COOKIE = "PREF=hl=en;";
	private static final String SESSION_COOKIE_NAME = "VISITOR_INFO1_LIVE";

	private YTUrl ytUrl;
	private String sts;
	private String video_page;
	private String video_info_page;
	private String embed_page;
	private ArrayList<YTFormat> yt_formats;
	private YTPlayer player;
	private String player_url;
	private String title;
	private String nextVideo;
	private String author;
	private String sessionCookie;

	/**
	 * Main class used for parsing and extraction
	 * @param ytUrl YTUrl instance that points to the url to be extracted
	 */
	public YTExtractor(YTUrl ytUrl) {
		this.ytUrl = ytUrl;
	}

	public ArrayList<YTFormat> getFormats() throws IOException {
		if (yt_formats == null) {
			// Search for formats and merge them
			StringBuilder formats = new StringBuilder();
			Matcher m;
			m = Pattern.compile("\"(?:adaptive_fmts|url_encoded_fmt_stream_map)\"\\s*:\\s*\"(?<formats>[^\"]*)\"").matcher(getVideoPage());
			while (m.find()) {
				if (formats.length() > 0) formats.append(",");
				formats.append(m.group("formats").replace("\\u0026", "&"));
			}
			if (formats.length() == 0) {
				// Try with /get_video_info
				m = Pattern.compile("(?:adaptive_fmts|url_encoded_fmt_stream_map)\\s*=\\s*(?<formats>[^&]*)").matcher(getVideoInfoPage());
				while (m.find()) {
					if (formats.length() > 0) formats.append(",");
					formats.append(URLDecoder.decode(m.group("formats"), StandardCharsets.UTF_8));
				}
			}

			yt_formats = new ArrayList<>();
			for (String format : formats.toString().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
				if (format.isEmpty()) continue;
				try {
					YTFormat ytf = new YTFormat(format);
					if (!yt_formats.contains(ytf))
						yt_formats.add(ytf);
				}
				catch (Exception e) {
					// Invalid format
				}
			}
			yt_formats.sort(YTFormat.YTFormatComparator);
		}
		return yt_formats;
	}

	public YTPlayer getPlayer() throws IOException, ScriptException {
		if (player == null) {
			player = new YTPlayer(getPlayerURL());
		}
		return player;
	}

	public String getTitle() throws IOException {
		if (title == null) {
			Pattern regex = Pattern.compile("\"title\"\\s*:\\s*\"(?<title>.+?)(?<![^\\\\]\\\\)\"");
			Matcher m = regex.matcher(getVideoPage());
			boolean found = true;
			if (!m.find()) {
				m = regex.matcher(getEmbedPage());
				if (!m.find()) found = false;
			}
			if (found) title = Utils.unescapeJavaScript(m.group("title"));
			else title = "";
		}
		return title;
	}

	public YTExtractor getNextVideo() throws IOException {
		if (nextVideo == null) {
			nextVideo = "";
			Matcher m = Pattern.compile("/watch\\?v=(?<id>[0-9A-Za-z_-]+)").matcher(getVideoPage());
			String newId;
			while (m.find()) {
				newId = m.group("id");
				if (!newId.equals(ytUrl.getId())) {
					nextVideo = ytUrl.getProtocol() + "//www.youtube.com/watch?v=" + newId;
					break;
				}
			}
		}
		YTExtractor nextExtractor = new YTExtractor(YTUrl.fromUrl(nextVideo));
		nextExtractor.sessionCookie = this.sessionCookie;
		return nextExtractor;
	}

	public String getAuthor() throws IOException {
		if (author == null) {
			Matcher m;
			m = Pattern.compile("\"author\"\\s*:\\s*\"(?<author>.+?)(?<![^\\\\]\\\\)\"").matcher(getVideoPage());
			if (m.find()) {
				author = Utils.unescapeJavaScript(m.group("author"));
			}
			else {
				m = Pattern.compile("author=(?<author>[^&]*)").matcher(getVideoInfoPage());
				if (m.find()) author = URLDecoder.decode(m.group("author"), StandardCharsets.UTF_8);
			}
		}
		return author;
	}

	public String getThumbnailUrl() {
		return ytUrl.getProtocol() + "//img.youtube.com/vi/" + ytUrl.getId() + "/mqdefault.jpg";
	}

	public String getFullThumbnailUrl() {
		return ytUrl.getProtocol() + "//img.youtube.com/vi/" + ytUrl.getId() + "/maxresdefault.jpg";
	}

	public YTUrl getYTUrl() {
		return ytUrl;
	}

	private String getVideoPage() throws IOException {
		if (video_page == null) {
			Map<String, String> responseCookies = new LinkedHashMap<>();
			String requestCookies = EN_US_COOKIE + ((sessionCookie != null && !sessionCookie.isEmpty()) ? " " + SESSION_COOKIE_NAME + "=" + sessionCookie : "");
 			video_page = Utils.downloadURLToString(ytUrl.getUrl(), requestCookies, responseCookies);

 			// Also set session cookie
 			if (sessionCookie == null && responseCookies.containsKey(SESSION_COOKIE_NAME)) {
 				sessionCookie = responseCookies.get(SESSION_COOKIE_NAME);
		    }
		}
		return video_page;
	}

	private String getVideoInfoPage() throws IOException {
		if (video_info_page == null) {
			for (String el : el_array) {
				// Try multiple urls until it works
				video_info_page = Utils.downloadURLToString(ytUrl.getProtocol() + "//www.youtube.com/get_video_info?video_id=" + ytUrl.getId() + "&hl=en_US" + el + "&sts=" + getSTS());
				if (video_info_page.contains("adaptive_fmts") || video_info_page.contains("url_encoded_fmt_stream_map")) break;
			}
		}
		return video_info_page;
	}

	private String getEmbedPage() throws IOException {
		if (embed_page == null) {
			embed_page = Utils.downloadURLToString(ytUrl.getProtocol() + "//www.youtube.com/embed/" + ytUrl.getId(), EN_US_COOKIE);
		}
		return embed_page;
	}

	private String getSTS() throws IOException {
		if (sts == null) {
			Pattern regex = Pattern.compile("\"sts\"\\s*:\\s*\"?(?<sts>[^\",}]+)\"?");
			Matcher m = regex.matcher(getVideoPage());
			boolean found = true;
			if (!m.find()) {
				// Also search in embed page
				m = regex.matcher(getEmbedPage());
				if (!m.find()) found = false;
			}
			if (found) sts = m.group("sts");
			else sts = "";
		}
		return sts;
	}

	private String getPlayerURL() throws IOException {
		if (player_url == null) {
			Pattern regex = Pattern.compile("\"js\"\\s*:\\s*\"(?<url>[^\"]+)\"");
			Matcher m = regex.matcher(getVideoPage());
			boolean found = true;
			if (!m.find()) {
				// Also search in embed page
				m = regex.matcher(getEmbedPage());
				if (!m.find()) found = false;
			}
			if (found) {
				player_url = m.group("url").replace("\\/", "/");

				URL check;
				if (!player_url.startsWith("http")) check = new URL(ytUrl.getProtocol() + "//" + player_url);
				else check = new URL(player_url);
				if (check.getHost() == null || check.getHost().isEmpty()) player_url = "www.youtube.com" + player_url;

				player_url = ytUrl.getProtocol() + "//" + player_url;

			}
			else player_url = "";
		}
		return player_url;
	}

}
