package com.nexus.casty;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class Utils {
	static Map<String, String> parseQuery(String query) {
		Map<String, String> map = new HashMap<>();
		for (String pair : query.split("&")) {
			String [] array = pair.split("=");
			if (array.length != 2) continue;
			map.put(array[0], array[1]);
		}

		return map;
	}

	static String downloadURLToString(String url) throws IOException {
		int len, BUF_LEN = 1024 * 1024;
		StringWriter output = new StringWriter();

		InputStreamReader input = new InputStreamReader(new URL(url).openStream(), "UTF-8");

		char[] buf = new char[BUF_LEN];
		while ((len = input.read(buf, 0, BUF_LEN)) != -1) output.write(buf, 0, len);

		return output.toString();
	}

	static void downloadURLToFile(String url, File file) throws IOException {
		int len, BUF_LEN = 1024 * 1024;

		System.out.print("Downloading \"" + url + "\"...");

		InputStream input = new URL(url).openStream();
		FileOutputStream output = new FileOutputStream(file);

		byte[] buf = new byte[BUF_LEN];
		while ((len = input.read(buf, 0, BUF_LEN)) != -1) output.write(buf, 0, len);

		System.out.println("OK");
	}
}
