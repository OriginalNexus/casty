package com.nexus.casty.cache;

import com.nexus.casty.song.SongData;

public class CacheEntry {
    String id;
    SongData data = null;

    public CacheEntry(String id) {
        this.id = id;
    }
}
