package com.nexus.casty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Utils {

	public static Map<String, String> parseQuery(@Nullable String query) {
		Map<String, String> map = new HashMap<>();
		if (query != null) {
			for (String pair : query.split("&")) {
				String[] array = pair.split("=");
				if (array.length != 2) continue;
				map.put(array[0], array[1]);
			}
		}
		return map;
	}

	public static void streamCopy(@NotNull InputStream in, @NotNull OutputStream out) throws IOException {
		int count, BUFFER_SIZE = 1024 * 1024;
		byte[] buffer = new byte[BUFFER_SIZE];
		while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) out.write(buffer, 0, count);
	}

	public static String streamToString(@NotNull InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		streamCopy(input, output);
		return output.toString(StandardCharsets.UTF_8);
	}

	public static void fileToStream(@NotNull File file, @NotNull OutputStream output) throws IOException {
		try (FileInputStream fs = new FileInputStream(file)) {
			streamCopy(fs, output);
		}
	}

	public static void downloadFile(@NotNull String url, @NotNull File file) throws IOException {
		try (FileOutputStream fs = new FileOutputStream(file)) {
			URLConnection con = new URL(url).openConnection();
			con.setRequestProperty("range", "bytes=0-");
			streamCopy(con.getInputStream(), fs);
		}
	}

	public static String stringToBase36(@NotNull String s) {
		return new BigInteger(s.getBytes(StandardCharsets.UTF_8)).toString(36);
	}

	public static String base36ToString(@NotNull String s) {
		return new String(new BigInteger(s.toLowerCase(), 36).toByteArray(), StandardCharsets.UTF_8);
	}

}
