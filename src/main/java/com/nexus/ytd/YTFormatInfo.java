package com.nexus.ytd;

public class YTFormatInfo {

    public enum YTFormatType {
        AUDIO, VIDEO, AV, AV_LIVE, UNKNOWN;

        @Override
        public String toString() {
            switch (this) {
                case AUDIO: return "Audio";
                case VIDEO: return "Video";
                case AV: return "A/V";
                case AV_LIVE: return "Live";
                case UNKNOWN: return "Unknown";
                default: return super.toString();
            }
        }
    }

    public final YTFormatType Type;
    public final String Container;
    public final String AudioEncoding;
    public final String AudioBitrate;
    public final String VideoResolution;
    public final String VideoEncoding;
    public final String VideoProfile;
    public final String VideoBitrate;

    YTFormatInfo(YTFormatType type, String container, String vResolution, String vEncoding, String vProfile, String vBitrate, String aEncoding, String aBitrate) {
        Type = type;
        Container = container;
        VideoResolution = vResolution;
        VideoEncoding = vEncoding;
        VideoProfile = vProfile;
        VideoBitrate = vBitrate;
        AudioEncoding = aEncoding;
        AudioBitrate = aBitrate;
    }

    YTFormatInfo(String container, String vResolution, String vEncoding, String vProfile, String vBitrate) {
        this(YTFormatType.VIDEO, container, vResolution, vEncoding, vProfile, vBitrate, null, null);
    }

    YTFormatInfo(String container, String aEncoding, String aBitrate) {
        this(YTFormatType.AUDIO, container, null, null, null, null, aEncoding, aBitrate);
    }

}
