package com.nexus.casty.song;

public class SongInfo {
	public String title;
	public String source;
	public String sourceName;
	public String thumbnail;
	public String thumbnailFull;
	public String download;

	SongInfo() {}

	SongInfo(SongData songInfo) {
		this.title = songInfo.title;
		this.source = songInfo.source;
		this.sourceName = songInfo.sourceName;
		this.thumbnail = songInfo.thumbnail;
		this.thumbnailFull = songInfo.thumbnailFull;
		this.download = songInfo.download;
	}
}