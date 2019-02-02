package com.nexus.ytd;

import java.util.LinkedHashMap;
import java.util.Map;

public class YTFormats {

    private YTFormats() {}

    public static final Map<String, YTFormatInfo> ItagsMap = new LinkedHashMap<>() {
        @Override
        public YTFormatInfo get(Object key) {
            return this.containsKey(key) ? super.get(key) : get("0");
        }
    };

    static {
        // Invalid/Unknown format
        ItagsMap.put("0", new YTFormatInfo(YTFormatInfo.YTFormatType.UNKNOWN, "Unknown", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown", "Unknown"));

        // Formats from https://en.wikipedia.org/wiki/YouTube
        // Non-DASH
        ItagsMap.put("17", new YTFormatInfo(YTFormatInfo.YTFormatType.AV, "3GP", "144p", "MPEG-4 Visual", "Simple", "0.05", "AAC", "24"));
        ItagsMap.put("36", new YTFormatInfo(YTFormatInfo.YTFormatType.AV, "3GP", "240p", "MPEG-4 Visual", "Simple", "0.175", "AAC", "32"));
        ItagsMap.put("18", new YTFormatInfo(YTFormatInfo.YTFormatType.AV, "MP4", "360p", "H.264", "Baseline", "0.5", "AAC", "96"));
        ItagsMap.put("22", new YTFormatInfo(YTFormatInfo.YTFormatType.AV, "MP4", "720p", "H.264", "High", "2-3", "AAC", "192"));
        ItagsMap.put("43", new YTFormatInfo(YTFormatInfo.YTFormatType.AV, "WebM", "360p", "VP8", "N/A", "0.5-0.75", "Vorbis", "128"));

        // DASH (video only)
        ItagsMap.put("160", new YTFormatInfo("MP4", "144p 15 fps", "H.264", "Main", "0.1"));
        ItagsMap.put("133", new YTFormatInfo("MP4", "240p", "H.264", "Main", "0.2–0.3"));
        ItagsMap.put("134", new YTFormatInfo("MP4", "360p", "H.264", "Main", "0.3–0.4"));
        ItagsMap.put("135", new YTFormatInfo("MP4", "480p", "H.264", "Main", "0.5–1"));
        ItagsMap.put("136", new YTFormatInfo("MP4", "720p", "H.264", "Main", "1–1.5"));
        ItagsMap.put("298", new YTFormatInfo("MP4", "720p HFR", "H.264", "Main", "3–3.5"));
        ItagsMap.put("137", new YTFormatInfo("MP4", "1080p", "H.264", "High", "2.5–3"));
        ItagsMap.put("299", new YTFormatInfo("MP4", "1080p HFR", "H.264", "High", "5.5"));
        ItagsMap.put("264", new YTFormatInfo("MP4", "1440p", "H.264", "High", "4–4.5"));
        ItagsMap.put("266", new YTFormatInfo("MP4", "2160p", "H.264", "High", "12.5–16"));
        ItagsMap.put("138", new YTFormatInfo("MP4", "4320p", "H.264", "High", "13.5–25"));
        ItagsMap.put("278", new YTFormatInfo("WebM", "144p 15 fps", "VP9", "Profile 0", "0.08"));
        ItagsMap.put("242", new YTFormatInfo("WebM", "240p", "VP9", "Profile 0", "0.1–0.2"));
        ItagsMap.put("243", new YTFormatInfo("WebM", "360p", "VP9", "Profile 0", "0.25"));
        ItagsMap.put("244", new YTFormatInfo("WebM", "480p", "VP9", "Profile 0", "0.5"));
        ItagsMap.put("247", new YTFormatInfo("WebM", "720p", "VP9", "Profile 0", "0.7–0.8"));
        ItagsMap.put("248", new YTFormatInfo("WebM", "1080p", "VP9", "Profile 0", "1.5"));
        ItagsMap.put("271", new YTFormatInfo("WebM", "1440p", "VP9", "Profile 0", "9"));
        ItagsMap.put("313", new YTFormatInfo("WebM", "2160p", "VP9", "Profile 0", "13–15"));
        ItagsMap.put("272", new YTFormatInfo("WebM", "4320p", "VP9", "Profile 0", "20–25"));
        ItagsMap.put("302", new YTFormatInfo("WebM", "720p HFR", "VP9", "Profile 0", "2.5"));
        ItagsMap.put("303", new YTFormatInfo("WebM", "1080p HFR", "VP9", "Profile 0", "5"));
        ItagsMap.put("308", new YTFormatInfo("WebM", "1440p HFR", "VP9", "Profile 0", "10"));
        ItagsMap.put("315", new YTFormatInfo("WebM", "2160p HFR", "VP9", "Profile 0", "20–25"));
        ItagsMap.put("330", new YTFormatInfo("WebM", "144p HDR, HFR", "VP9", "Profile 2", "0.08"));
        ItagsMap.put("331", new YTFormatInfo("WebM", "240p HDR, HFR", "VP9", "Profile 2", "0.1-0.15"));
        ItagsMap.put("332", new YTFormatInfo("WebM", "360p HDR, HFR", "VP9", "Profile 2", "0.25"));
        ItagsMap.put("333", new YTFormatInfo("WebM", "480p HDR, HFR", "VP9", "Profile 2", "0.5"));
        ItagsMap.put("334", new YTFormatInfo("WebM", "720p HDR, HFR", "VP9", "Profile 2", "1"));
        ItagsMap.put("335", new YTFormatInfo("WebM", "1080p HDR, HFR", "VP9", "Profile 2", "1.5-2"));
        ItagsMap.put("336", new YTFormatInfo("WebM", "1440p HDR, HFR", "VP9", "Profile 2", "5-7"));
        ItagsMap.put("337", new YTFormatInfo("WebM", "2160p HDR, HFR", "VP9", "Profile 2", "12-14"));

        // DASH (audio only)
        ItagsMap.put("140", new YTFormatInfo("MP4","AAC", "128"));
        ItagsMap.put("171", new YTFormatInfo("WebM","Vorbis", "128"));
        ItagsMap.put("249", new YTFormatInfo("WebM","Opus", "48"));
        ItagsMap.put("250", new YTFormatInfo("WebM","Opus", "64"));
        ItagsMap.put("251", new YTFormatInfo("WebM", "Opus", "160"));

        // Live streaming
        ItagsMap.put("91", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "144p", "H.264", "Main", "0.1", "AAC", "48"));
        ItagsMap.put("92", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "240p", "H.264", "Main", "0.15–0.3", "AAC", "48"));
        ItagsMap.put("93", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "360p", "H.264", "Main", "0.5–1", "AAC", "128"));
        ItagsMap.put("94", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "480p", "H.264", "Main", "0.8–1.25", "AAC", "128"));
        ItagsMap.put("95", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "720p", "H.264", "Main", "1.5–3", "AAC", "256"));
        ItagsMap.put("96", new YTFormatInfo(YTFormatInfo.YTFormatType.AV_LIVE, "TS", "1080p", "H.264", "High", "2.5–6", "AAC", "256"));

    }

}
