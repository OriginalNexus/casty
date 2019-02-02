package com.nexus.casty;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class Utils {

	public static String stringToBase36(String s) {
		return new BigInteger(s.getBytes(StandardCharsets.UTF_8)).toString(36);
	}

	public static String base36ToString(String s) throws NumberFormatException {
		return new String(new BigInteger(s.toLowerCase(), 36).toByteArray(), StandardCharsets.UTF_8);
	}

    public static void downloadToStream(String url, OutputStream outputStream) throws IOException {
		URLConnection con = new URL(url).openConnection();
		con.setRequestProperty("range", "bytes=0-");
		con.getInputStream().transferTo(outputStream);
	}
}
