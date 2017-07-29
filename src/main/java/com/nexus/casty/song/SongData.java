package com.nexus.casty.song;


public class SongData extends SongInfo {
	public boolean specialDemux = false;
	public String container;
	public String filename;

	public SongData() {}

	public SongData(SongData songData) {
		super(songData);
		this.specialDemux = songData.specialDemux;
		this.container = songData.container;
		this.filename = songData.filename;
	}
}
