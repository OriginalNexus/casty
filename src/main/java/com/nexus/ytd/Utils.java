package com.nexus.ytd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

class Utils {

	private static final String CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

    static String extractJSObject(String s, int index) {
        int cnt = 0;
        int pos = index;
        while (true) {
            int a1 = s.indexOf('{', pos);
            int a2 = s.indexOf('}', pos);
            int a3 = s.indexOf(';', pos);

            if (a3 == -1) {
                throw new UnknownError("Could not extract JS Object");
            }
            if (cnt == 0 && (a1 == -1 || a3 < a1)) {
                pos = a3 + 1;
                break;
            }
            if (a1 != -1 && (a2 == -1 || a1 < a2)) {
                cnt++; pos = a1 + 1;
            }
            if (a2 != -1 && (a1 == -1 || a2 < a1)){
                cnt--; pos = a2 + 1;
            }
        }

        return s.substring(index, pos);
    }

    static String downloadURLToString(String url) throws IOException {
		StringWriter output = new StringWriter();
		URLConnection con = new URL(url).openConnection();
		con.setRequestProperty("User-Agent", CHROME_USER_AGENT);
		InputStreamReader input = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
		input.transferTo(output);

		return output.toString();
	}

	static String unescapeJavaScript(String escaped) {
		return escaped.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\u0026", "&");
	}

}
