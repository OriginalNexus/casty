package com.nexus.casty.song;

public class SongInfo {
	public String title = "";
	public String source = "";
	public String sourceName = "";
	public String thumbnail = "img/generic-song.png";
	public String thumbnailFull = "";
	public String download = "";

	public SongInfo() {}

	public SongInfo(SongInfo songInfo) {
		this.title = songInfo.title;
		this.source = songInfo.source;
		this.sourceName = songInfo.sourceName;
		this.thumbnail = songInfo.thumbnail;
		this.thumbnailFull = songInfo.thumbnailFull;
		this.download = songInfo.download;
	}
}