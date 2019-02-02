package com.nexus.ytd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTUrl {

	private String id;
	private String protocol;

	public static YTUrl fromUrl(String url) {
		Matcher m;
		m = Pattern.compile("(?<protocol>http[s]?:)//(?:www\\.)?youtube\\.com/watch\\?\\S*?&?v=(?<id>[0-9A-Za-z_-]+)").matcher(url);
		if (m.find()) {
			YTUrl ytUrl = new YTUrl();
			ytUrl.id = m.group("id");
			ytUrl.protocol = m.group("protocol");
			return ytUrl;
		}
		return null;
	}

	private YTUrl() {}

	public String getId() {
		return id;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getUrl() { return protocol + "//www.youtube.com/watch?v=" + id; }
}
