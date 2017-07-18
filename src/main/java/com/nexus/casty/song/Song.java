package com.nexus.casty.song;

import com.nexus.casty.cache.CacheFile;

public class Song {
	public SongData data;
	public String streamUrl;
	public CacheFile file;
	public boolean isNew = false;
	public String nextUrl;
}
