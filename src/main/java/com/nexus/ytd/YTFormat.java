package com.nexus.ytd;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YTFormat {

	static final Comparator<YTFormat> YTFormatComparator = (f1, f2) -> {
        if (f1.getItag().equals(f2.getItag()))
            return 0;

        for (Map.Entry<String, YTFormatInfo> entry : YTFormats.ItagsMap.entrySet()) {
            if (entry.getKey().equals(f1.getItag()))
                return -1;
            else if (entry.getKey().equals(f2.getItag()))
                return 1;
        }
        return f1.getItag().compareTo(f2.getItag());
    };

    private String url;
    private String s;
    private String itag = "0";

    YTFormat(String str) {
        Pattern regex = Pattern.compile("(?<key>[^&=\\s]+)\\s*=\\s*(?<value>[^&]*)");
        Matcher m = regex.matcher(str);
        while (m.find()) {
            if (m.group("key").equals("url")) {
				url = URLDecoder.decode(m.group("value"), StandardCharsets.UTF_8);
				if (!url.contains("ratebypass=yes"))
				    url += "&ratebypass=yes";
			}
            if (m.group("key").equals("itag"))
                itag = m.group("value");
            if (m.group("key").equals("s"))
                s = m.group("value");
        }
    }

	@Override
	public boolean equals(Object obj) {
		return obj instanceof YTFormat && this.itag.equals(((YTFormat) obj).itag);
	}

	public String getURL() {
        return url;
    }

    public String getItag() {
        return itag;
    }

    public boolean isEncrypted() {
        return s != null && !s.isEmpty();
    }

    String getEncryptedSignature() {
        return s;
    }

    void decrypt(String decryptedSignature) {
        if (!isEncrypted())
            return;
        url += "&signature=" + decryptedSignature;
        s = null;
    }

}
