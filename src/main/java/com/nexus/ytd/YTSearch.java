package com.nexus.ytd;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTSearch {

	public static YTSearchResult[] performSearch(String query) throws IOException {
		String resultsPage = Utils.downloadURLToString("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));

		Map<String, YTSearchResult> videos = new LinkedHashMap<>();

		Matcher m = Pattern.compile(
				"\"videoRenderer\"\\s*:\\s*\\{[\\S\\s]*?" +
				"\"videoId\"\\s*:\\s*\"(?<id>[^\"]+)\"[\\S\\s]*?" +
				"\"title\"\\s*:\\s*\\{[\\S\\s]*?\"simpleText\"\\s*:\\s*\"(?<title>.+?)(?<![^\\\\]\\\\)\"[\\S\\s]*?" +
				"\"longBylineText\"\\s*:\\s*\\{[\\S\\s]*?\"text\"\\s*:\\s*\"(?<author>.+?)(?<![^\\\\]\\\\)\"")
				.matcher(resultsPage);

		while (m.find()) {
			String id = m.group("id");
			String title = Utils.unescapeJavaScript(m.group("title"));
			String author = Utils.unescapeJavaScript(m.group("author"));

			if (id != null && !id.isEmpty()) {
				videos.computeIfAbsent(id, key -> {
					YTSearchResult result = new YTSearchResult();
					result.id = key;
					result.title = title;
					result.author = author;
					result.thumbnail = "http://img.youtube.com/vi/" + key + "/mqdefault.jpg";
					return result;
				});
			}
		}

		Object[] array = videos.values().toArray();
		return Arrays.copyOf(array, array.length, YTSearchResult[].class);

	}
}
